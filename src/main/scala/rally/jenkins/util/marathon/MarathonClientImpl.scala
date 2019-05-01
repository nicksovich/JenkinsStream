package rally.jenkins.util.marathon

import akka.http.scaladsl.model.{HttpMethods, HttpRequest, StatusCodes}
import rally.jenkins.util.model.{MarathonApp, MarathonApps, RawBuildInfo}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.io.Source

class MarathonClientImpl(
  tenantParam: String)(
  implicit val materializer: ActorMaterializer,
  executionContext: ExecutionContext,
  actorSystem: ActorSystem
) extends MarathonClient {

  def tenant: String = tenantParam

  def baseUrl: String = s"http://mesos.$tenantParam.rally-dev.com:8080"

  def getEnv(): Future[Seq[MarathonApp]] = {
    val url = baseUrl + "/v2/apps"
    val request = HttpRequest(
      method = HttpMethods.GET,
      uri = url,
    )

    implicit val format = jsonFormat6(MarathonApp)
    implicit val format2 = jsonFormat1(MarathonApps)

    (for {
      response <- Http().singleRequest(request)
      marathonApps <- Unmarshal(response.entity).to[MarathonApps]
    } yield {
      marathonApps.apps.sortBy(_.id)
    }).recover {
      case ex =>
        println(ex.getMessage)
        Seq.empty[MarathonApp]
    }
  }

  def updateEnv(appId: String, env: Map[String, String]): Future[MarathonApp] = ???

  def properSetup(tenant: String): Future[Seq[MarathonApp]] = {
      val file = Source.fromFile("working-active-and-sync.json")
      val fileContents = file.getLines.mkString.replace("{{tenant}}", tenant)

      implicit val format = jsonFormat6(MarathonApp)
      implicit val format2 = jsonFormat1(MarathonApps)

    Unmarshal(fileContents).to[MarathonApps].map(_.apps.sortBy(_.id))
  }

  def appWithoutTenant(id: String): String = id.split("/").tail.tail.mkString("/")
}
