package rally.jenkins.util

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory

import scala.util.{Failure, Success}

object Main extends App {

  val config = ConfigFactory.load
  val jenkinsConfig = JenkinsConfig(config.getString("url"), config.getString("username"), config.getString("token"))

  private implicit val system = ActorSystem()
  private implicit val materializer = ActorMaterializer()
  private implicit val executionContext = system.dispatcher

  val jenkinsClient = new JenkinsClientImpl(jenkinsConfig)

  implicit val defaultHandler = JenkinsClient.continueOnNonSuccessfulBuild

  (for {
    createTenant <- jenkinsClient.createTenant("engage", "3 hours", "dev")(JenkinsClient.stopOnNonSuccessfulBuild)
    tenant = createTenant.description
    engageStack <- jenkinsClient.deployStack(tenant, "engage")(defaultHandler)
    engineStack <- jenkinsClient.deployStack(tenant, "engine")(defaultHandler)
    rewardsUI <- jenkinsClient.deployComponent("rewards-ui", "8.9.0", tenant)(defaultHandler)
  } yield {
    println(s"createTenant: $createTenant")
    println(s"engageStack: $engageStack")
    println(s"engineStack: $engineStack")
    println(s"rewardsUI: $rewardsUI")
    system.terminate()
  }).recover {
    case e: Exception =>
      println(e.getMessage)
      system.terminate()
  }
//
//  jenkinsClient.deployComponent("rewards-ui", "8.9.0", "clever-spring") onComplete {
//    case Success(buildInfo) => println(buildInfo)
//    case Failure(ex) => println(ex.getMessage)
//  }
}
