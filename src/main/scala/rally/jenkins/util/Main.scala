package rally.jenkins.util

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.{Accept, BasicHttpCredentials}
import akka.http.scaladsl.model.{HttpMethods, HttpProtocols, HttpRequest, HttpResponse, MediaTypes}
import akka.stream.ActorMaterializer

import scala.concurrent.Future
import scala.util.{Failure, Success}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import com.typesafe.config.{Config, ConfigFactory}
import spray.json.DefaultJsonProtocol._

object Main extends App {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher

  val baseJenkinsUrl = "https://ci.rally-dev.com"
  def jobPath(job: String, branch: String) = s"teams-deploys/job/deploys/job/$job/job/$branch"
  val lastBuild = "lastBuild/api/json"

  val job = "CreateTenant"
  val branch = "master"
  val url = List(baseJenkinsUrl, jobPath(job, branch), lastBuild).mkString("/")
  println(url)

  val config = ConfigFactory.load
  val username = config.getString("username")
  val password = config.getString("token")

  val credentials = BasicHttpCredentials(username, password)

  val request = HttpRequest(
    method = HttpMethods.GET,
    uri = url,
    headers = List(Accept(MediaTypes.`application/json`)),
    protocol = HttpProtocols.`HTTP/1.1`
  )
    .addCredentials(credentials)

  val responseFuture: Future[HttpResponse] = Http().singleRequest(request)

  case class JobInfo(building: Boolean, result: Option[String], queueId: Int, description: Option[String])
  implicit val jobInfoFormat = jsonFormat4(JobInfo)

  responseFuture
    .onComplete {
      case Success(res) =>
        Unmarshal(res.entity).to[JobInfo].onComplete {
          case Success(jobInfo) =>
            println(jobInfo)
            system.terminate()
          case Failure(err) =>
            sys.error(err.getMessage)
            system.terminate()
        }
        system.terminate()
      case Failure(_) =>
        sys.error("something wrong")
        system.terminate()
    }
}
