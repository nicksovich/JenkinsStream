package rally.jenkins.util

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import rally.jenkins.util.model.BuildInfo

import scala.concurrent.ExecutionContext

object Main extends App {

  val config = ConfigFactory.load
  val jenkinsConfig = JenkinsConfig(config.getString("url"), config.getString("username"), config.getString("token"))

  private implicit val system: ActorSystem = ActorSystem()
  private implicit val materializer: ActorMaterializer = ActorMaterializer()
  private implicit val executionContext: ExecutionContext = system.dispatcher

  val jenkinsClient = new JenkinsClientImpl(jenkinsConfig)

  implicit val defaultHandler: BuildInfo => BuildInfo = JenkinsClient.continueOnNonSuccessfulBuild

  (for {
    createTenant <- jenkinsClient.createTenant("engage", "3 hours", "dev")(JenkinsClient.stopOnNonSuccessfulBuild)
    tenant = createTenant.description
    engageStack <- jenkinsClient.deployStack(tenant, "engage")
    engineStack <- jenkinsClient.deployStack(tenant, "engine")
  } yield {
    println(s"createTenant: $createTenant")
    println(s"engageStack: $engageStack")
    println(s"engineStack: $engineStack")
    system.terminate()
  }).recover {
    case e: Exception =>
      println(e.getMessage)
      system.terminate()
  }
}
