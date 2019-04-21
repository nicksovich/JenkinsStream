package rally.jenkins.util

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import rally.jenkins.util.model.JobInfo
import spray.json.DefaultJsonProtocol
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._

import scala.concurrent.Future

class JenkinsClientImpl(jenkinsConfig: JenkinsConfig) extends JenkinsClient {

  private implicit val system = ActorSystem()
  private implicit val materializer = ActorMaterializer()
  private implicit val executionContext = system.dispatcher

  private val credentials = BasicHttpCredentials(jenkinsConfig.username, jenkinsConfig.token)

  private val lastBuild = "lastBuild/api/json"
  private val buildInfo = "api/json"
//  private val runBuild = "build" https://stackoverflow.com/questions/51638867/how-to-get-the-right-build-number-for-a-build-triggered-in-jenkins-by-remote-api
  private val runBuildWithParameters = "buildWithParameters"

  private def jobPath(job: String, branch: String) = s"teams-deploys/job/deploys/job/$job/job/$branch"

  private def buildParams(params: Map[String, String]): String = {
    val paramList = (params.toList map { case (key, value) =>  s"""{"name": "$key", "value": "$value"}"""}).mkString(", ")
    s"""{"parameter": [$paramList]}"""
  }

  private def triggerJob(jobName: String, jobBranch: String, parameters: String, jobType: String): Future[Either[QueueId, Error]] = {
    val url = List(jenkinsConfig.baseURL, jobPath(jobName, jobBranch), jobType).mkString("/")
    val request = HttpRequest(
      method = HttpMethods.POST,
      uri = url,
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
            case Some(header) => Left(header.value())
            case None => Right(-1)
          }
        case _ => Right(response.status.intValue())
      }
    }
  }

  private def runJob(jobName: String, jobBranch: String, parameters: String): Future[Either[QueueId, Error]] = {
    triggerJob(jobName, jobBranch, parameters, runBuildWithParameters)
  }

  def createTenant(stacks: String, lifespan: String, environment: String): Future[Either[QueueId, Error]] = {
    val parameters = buildParams(Map("stacks" -> stacks, "lifespan" -> lifespan, "environment" -> environment))
    runJob("CreateTenant", "master", parameters)
  }

  def destroyTenant(tenant: String): Future[Either[QueueId, Error]] = {
    val parameters = buildParams(Map("tenant" -> tenant))
    runJob("DestroyTenant", "master", parameters)
  }

  def deployStack(): Future[QueueId] = ???

  def deployComponent(component: String, version: String, tenant: String): Future[Either[QueueId, Error]] = {
    val parameters = buildParams(Map("component" -> component, "version" -> version, "tenant" -> tenant))
    runJob("DeployComponent", "master", parameters)
  }

  def waitForJobToFinish(queueId: QueueId): Future[Either[Status, Error]] = {
    val url = queueId + "api/json?pretty=true"
    val request = HttpRequest(
      method = HttpMethods.GET,
      uri = url
    ).addCredentials(credentials)

    case class JobNumber(number: Int)
    case object JobNumber extends SprayJsonSupport with DefaultJsonProtocol {

      implicit val jobNumberFormat = jsonFormat1(JobNumber.apply)
    }

    for {
      response <- Http().singleRequest(request)
      jobNumber <- Unmarshal(response).to[JobNumber]
    } yield {
      response.status match {
        case StatusCodes.Accepted => Left(jobNumber.number.toString)
        case _ => Right(response.status.intValue())
      }
    }
  }

  def getLastJobInfo(jobName: String): Future[JobInfo] = {
    val url = List(jenkinsConfig.baseURL, jobPath(jobName, "master"), lastBuild).mkString("/")
    val request = HttpRequest(
      method = HttpMethods.GET,
      uri = url
    ).addCredentials(credentials)

    implicit val jobInfo = jsonFormat2(JobInfo)

    for {
      response <- Http().singleRequest(request)
      jobInfo <- Unmarshal(response.entity).to[JobInfo]
    } yield {
      jobInfo
    }
  }

  def getLastNJobInfos(jobName: String, n: Int): Future[Seq[JobInfo]] = {
    for {
      lastJobInfo <- getLastJobInfo(jobName)
      lastNJobsInfos <- getJobsInfos(jobName, lastJobInfo.number - n, lastJobInfo.number)
    } yield {
      lastNJobsInfos
    }
  }

  def getJobInfo(jobName: String, buildNumber: Int): Future[JobInfo] = {
    val url = List(jenkinsConfig.baseURL, jobPath(jobName, "master"), buildNumber.toString, buildInfo).mkString("/")
    val request = HttpRequest(
      method = HttpMethods.GET,
      uri = url
    ).addCredentials(credentials)

    implicit val jobInfo = jsonFormat2(JobInfo)

    for {
      response <- Http().singleRequest(request)
      jobInfo <- Unmarshal(response.entity).to[JobInfo]
    } yield {
      jobInfo
    }
  }

  def getJobsInfos(jobName: String, startBuildNumber: Int, endBuildNumber: Int): Future[Seq[JobInfo]] = {
    val jobInfo = (number: Int) => getJobInfo(jobName, number)
    val jobInfos = startBuildNumber to endBuildNumber map jobInfo
    Future.sequence(jobInfos)
  }
}
