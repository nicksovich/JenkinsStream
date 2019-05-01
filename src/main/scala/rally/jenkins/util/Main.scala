package rally.jenkins.util

import akka.Done
import akka.http.scaladsl.Http
import script.{ActiveAndSyncSetup, Manifest => Man}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._

import scala.concurrent.Future
import scala.io.StdIn

object Main extends App with Context {

  val route =
    path("manifest") {
      post {
        entity(as[String]) { tenant =>
          val saved: Future[Done] = new Man(tenant).run
          onComplete(saved) { _ =>
            complete(StatusCodes.OK)
          }
        }
      }
    } ~
    path("activeAndSync") {
      post {
        val saved: Future[Done] = new ActiveAndSyncSetup().run
        onComplete(saved) { _ =>
          complete(StatusCodes.OK)
        }
      }
    }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done
}
