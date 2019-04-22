package rally.jenkins.util

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object Main extends App {

  val config = ConfigFactory.load
  val jenkinsConfig = JenkinsConfig(config.getString("url"), config.getString("username"), config.getString("token"))

  private implicit val system = ActorSystem()
  private implicit val materializer = ActorMaterializer()
  private implicit val executionContext = system.dispatcher

  val jenkinsClient = new JenkinsClientImpl(jenkinsConfig)

  system.terminate()


  // https://ci.rally-dev.com/teams-deploys/queue/item/29331/api/json?pretty=true
  // https://ci.rally-dev.com/teams-deploys/queue/api/json?pretty=true


//  val fut = jenkinsClient.getLastNJobInfos("DeployComponent", 10).onComplete {
//    case Success(jobInfos) => println(jobInfos);
//    case Failure(ex) => println(ex.getMessage);
//  }

//  // start deployComponent job
//  jenkinsClient.deployComponent("rewards-ui", "8.9.0", "clever-spring") map {
//    case Left(queueId) =>
//      // try to get buildId for at most 3 minutes
//    case Right(i) => throw new Exception(s"deploy component failed because of $i")
//  }


}
