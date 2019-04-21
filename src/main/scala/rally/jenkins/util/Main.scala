package rally.jenkins.util

import com.typesafe.config.ConfigFactory
import rally.jenkins.util.model.JobInfo

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

object Main extends App {

  val config = ConfigFactory.load
  val jenkinsConfig = JenkinsConfig(config.getString("url"), config.getString("username"), config.getString("token"))
  val jenkinsClient = new JenkinsClientImpl(jenkinsConfig)
  // https://ci.rally-dev.com/teams-deploys/queue/item/29331/api/json?pretty=true
  // https://ci.rally-dev.com/teams-deploys/queue/api/json?pretty=true

//  jenkinsClient.deployComponent("rewards-ui", "8.9.0", "clever-spring") map {
//    case Left(queueId) =>
//      Thread.sleep(5000)
//      jenkinsClient.waitForJobToFinish(queueId) map {
//      case Left(status) => println(status)
//      case Right(error) => println(error)
//    }
//    case Right(i) => println(i)
//  }

  jenkinsClient.getLastNJobInfos("DeployComponent", 10).onComplete {
    case Success(jobInfos) => println(jobInfos)
    case Failure(ex) => println(ex.getMessage)
  }
}
