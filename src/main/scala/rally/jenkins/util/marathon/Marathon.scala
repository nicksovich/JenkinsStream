package rally.jenkins.util.marathon

import akka.actor.ActorSystem
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import rally.jenkins.util.model.{MarathonApp, MarathonApps}
import spray.json.DefaultJsonProtocol.{jsonFormat1, jsonFormat6}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._

import scala.concurrent.ExecutionContext
import scala.io.Source

object Marathon extends App {
//
//  private implicit val system: ActorSystem = ActorSystem()
//  private implicit val materializer: ActorMaterializer = ActorMaterializer()
//  private implicit val executionContext: ExecutionContext = system.dispatcher
//
//  private val marathonClient: MarathonClient = new MarathonClientImpl("happy-sock")
//
//  val file = Source.fromFile("working-active-and-sync.json")
//  val fileContents = file.getLines.mkString
//
//  implicit val format = jsonFormat6(MarathonApp)
//  implicit val format2 = jsonFormat1(MarathonApps)
//
//  (for {
//    properApps <- Unmarshal(fileContents).to[MarathonApps]
//    checkedApps <- marathonClient.getEnv()
//  } yield {
//    def filteredApps(apps: Seq[MarathonApp]) = apps.map(a => (a.id, a.env.filter((keyValue: (String, String)) => keyValue._2.length < 30)))
//    val properFilteredApps = filteredApps(properApps.apps)
//    val checkedFilteredApps = filteredApps(checkedApps)
//
//    def appWithoutTenant(id: String): String = id.split("/").tail.tail.mkString("/")
//
//    properFilteredApps.foreach(properApp => {
//      checkedFilteredApps.find(checkedApp => {
//        appWithoutTenant(checkedApp._1) == appWithoutTenant(properApp._1) &&
//        checkedApp._2.get("VERSION") == properApp._2.get("VERSION")
//      }) match {
//        case None =>
//          println(s"Missing app ${properApp._1} version: ${properApp._2.get("VERSION")}")
//        case Some(app) => print("")
//      }
//    })
//    file.close
//    system.terminate
//  }).recover {
//    case ex =>
//      println(ex)
//      file.close
//      system.terminate
//  }
}
