package rally.jenkins.util

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext

trait Context {

  val config = ConfigFactory.load
  val jenkinsConfig = JenkinsConfig(config.getString("url"), config.getString("username"), config.getString("token"))

  val jenkinsClient = new JenkinsClientImpl(jenkinsConfig)

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = system.dispatcher

}
