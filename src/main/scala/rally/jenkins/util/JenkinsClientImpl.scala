package rally.jenkins.util

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import rally.jenkins.util.model.{BuildInfo, JobNumber, RawBuildInfo}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import rally.jenkins.util.enum.{BuildAborted, BuildFailure, BuildResult, BuildSuccess, JobName}
import spray.json.DefaultJsonProtocol._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class JenkinsClientImpl(jenkinsConfig: JenkinsConfig)(
  implicit val materializer: ActorMaterializer,
  executionContext: ExecutionContext,
  actorSystem: ActorSystem
) extends JenkinsClient {

  type QueueId = String

  private val credentials = BasicHttpCredentials(jenkinsConfig.username, jenkinsConfig.token)

  private val lastBuild = "lastBuild/api/json"
  private val buildInfo = "api/json"
  // https://stackoverflow.com/questions/51638867/how-to-get-the-right-build-number-for-a-build-triggered-in-jenkins-by-remote-api
  private val runBuildWithParameters = "buildWithParameters"

  def createTenant(stacks: String, lifespan: String, environment: String)
    (implicit handler: BuildInfo => BuildInfo): Future[BuildInfo] = {
    val parameters = buildParams(Map("stacks" -> stacks, "lifespan" -> lifespan, "environment" -> environment))
    runJob("CreateTenant", "master", parameters) map handler
  }

  def deployStack(tenant: String, stack: String)
    (implicit handler: BuildInfo => BuildInfo): Future[BuildInfo] = {
    val parameters = buildParams(Map("tenant" -> tenant, "stack" -> stack))
    runJob("DeployStack", "master", parameters) map handler
  }

  def deployComponent(component: String, version: String, tenant: String)
    (implicit handler: BuildInfo => BuildInfo): Future[BuildInfo] = {
    val parameters = buildParams(Map("component" -> component, "version" -> version, "tenant" -> tenant))
    runJob("DeployComponent", "master", parameters) map handler
  }

  def destroyTenant(tenant: String)(implicit handler: BuildInfo => BuildInfo): Future[BuildInfo] = {
    val parameters = buildParams(Map("tenant" -> tenant))
    runJob("DestroyTenant", "master", parameters) map handler
  }

  private def runJob(jobName: String, jobBranch: String, parameters: String): Future[BuildInfo] = {
    println(s"Running job $jobName with params: $parameters")
    (for {
      queueId <- triggerJob(jobName, jobBranch, parameters, runBuildWithParameters)
      _ <- Future { println(queueId) }
      buildInfo <- waitForJobToFinish(jobName, queueId)
      _ <- Future {
        println(buildInfo)
      }
    } yield buildInfo)
      .recover {
        case e: Exception =>
          println(e.getMessage)
          BuildInfo(JobName.fromString(jobName), -1, BuildFailure, "")
      }
  }

  private def triggerJob(jobName: String, jobBranch: String, parameters: String, jobType: String): Future[QueueId] = {
    val url = List(jenkinsConfig.baseURL, jobPath(jobName, jobBranch), jobType).mkString("/")
    val urlWithParams = s"$url?$parameters"
    println(urlWithParams)
    val request = HttpRequest(
      method = HttpMethods.POST,
      uri = urlWithParams,
      entity = FormData(
        "json" -> parameters
      ).toEntity
    ).addCredentials(credentials)

    for {
      response <- Http().singleRequest(request)
    } yield {
      response.status match {
        case StatusCodes.Created =>
          response.headers.find(p => p.name() == "Location") match {
            case Some(header) => header.value
            case None => throw new Exception("")
          }
        case _ => throw new Exception("")
      }
    }
  }

  /*
  Warning for those that dare inspect the function below. This is some blasphemous code that goes against every
  good practice that exists. We have Awaits, vars, and while loops. I stopped just short of writing a goto...
  This can be rewritten nicely using Akka streams or even Futures, but I'll have to do that at some point
  when I'm not sleep deprived.
   */

  private def waitForJobToFinish(jobName: String, queueLink: String): Future[BuildInfo] = {
    implicit val jobNumberFormat = jsonFormat1(JobNumber.apply)
    val maxAttempts = 360
    val timeBetweenAttempts = 5000
    // extract the number queueId from queueId link
    val splitQueueLink = queueLink.split("/")
    val queueId = splitQueueLink(splitQueueLink.length - 1).toInt

    var attempts = 0
    var buildId: Option[RawBuildInfo] = None
    while (attempts < maxAttempts && buildId.isEmpty) {
      val last10JobInfos = Await.result(getLastNJobInfos(jobName, 10), 10 seconds)
      buildId = last10JobInfos.find(b => b.queueId == queueId)
      if (buildId.isEmpty) {
        println("waiting for build to start...")
        Thread.sleep(timeBetweenAttempts) // sleep for a second
        attempts += 1
      }
    }

    attempts = 0
    var buildInfo: Option[BuildInfo] = None
    while (attempts < maxAttempts && buildInfo.isEmpty) {
      buildId match {
        case Some(RawBuildInfo(_, _, _, buildId, _, _)) =>
          println(s"Trying to get job for buildId $buildId")
          val rawBuildInfo: RawBuildInfo = Await.result(getJobInfo(jobName, buildId), 10 seconds)
          println(s"Got raw build info: $rawBuildInfo")
          rawBuildInfo.result match {
            case Some(result) if List(BuildSuccess, BuildFailure, BuildAborted).map(_.toString)
              .contains(result) => buildInfo = Some(
              BuildInfo(
                JobName.fromString(jobName),
                buildId,
                BuildResult.toBuildResult(result),
                rawBuildInfo.description.getOrElse("")
              )
            )
            case None => {
              println("waiting for build to finish...")
              Thread.sleep(timeBetweenAttempts) // sleep for a second
              attempts += 1
            }
          }
        case None => {
          println("waiting for build to finish...")
          Thread.sleep(timeBetweenAttempts) // sleep for a second
          attempts += 1
        }
      }
    }

    Future(buildInfo.get)
  }

  private def getLastJobInfo(jobName: String): Future[BuildInfo] = {
    println("getLastJobInfo")
    val url = List(jenkinsConfig.baseURL, jobPath(jobName, "master"), lastBuild).mkString("/")
    val request = HttpRequest(
      method = HttpMethods.GET,
      uri = url
    ).addCredentials(credentials)

    implicit val jobInfo = jsonFormat6(RawBuildInfo)
    println(url)

    for {
      response <- Http().singleRequest(request)
      jobInfo <- Unmarshal(response.entity).to[RawBuildInfo]
    } yield jobInfo match {
      case RawBuildInfo(description, _, _, buildId, _, result) => BuildInfo(JobName.fromString(jobName), buildId, BuildResult.toBuildResult(result.getOrElse("")), description.getOrElse(""))
      case _ => throw new Exception("TODO")
    }
  }

  private def getLastNJobInfos(jobName: String, n: Int): Future[Seq[RawBuildInfo]] = {
    println("getLastNJobInfos")
    for {
      lastJobInfo <- getLastJobInfo(jobName)
      lastNJobsInfos <- getJobsInfos(jobName, lastJobInfo.buildId - n, lastJobInfo.buildId)
    } yield lastNJobsInfos

  }

  private def getJobInfo(jobName: String, buildNumber: Int): Future[RawBuildInfo] = {
    println("getJobInfo")
    val url = List(jenkinsConfig.baseURL, jobPath(jobName, "master"), buildNumber.toString, buildInfo).mkString("/")
    val request = HttpRequest(
      method = HttpMethods.GET,
      uri = url
    ).addCredentials(credentials)
    println(url)
    implicit val rawJobInfo = jsonFormat6(RawBuildInfo)

    for {
      response <- Http().singleRequest(request)
      jobInfo <- Unmarshal(response.entity).to[RawBuildInfo]
    } yield jobInfo
  }

  private def getJobsInfos(jobName: String, startBuildNumber: Int, endBuildNumber: Int): Future[Seq[RawBuildInfo]] = {
    println("getJobInfos")
    val jobInfo = (number: Int) => getJobInfo(jobName, number)
    val jobInfos = startBuildNumber to endBuildNumber map jobInfo
    Future.sequence(jobInfos)
  }

  private def jobPath(job: String, branch: String) = s"teams-deploys/job/deploys/job/$job/job/$branch"

  private def buildParams(params: Map[String, String]): String = {
    (params.toList map { case (key, value) => s"$key=$value"}).mkString("&").replace(" ", "%20")
  }
}
