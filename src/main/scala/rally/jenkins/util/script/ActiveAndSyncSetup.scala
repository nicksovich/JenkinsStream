package rally.jenkins.util.script

import akka.Done
import rally.jenkins.util.{Context, JenkinsClient}

import scala.concurrent.Future

class ActiveAndSyncSetup() extends Script with Context {

  override def run: Future[Done] = for {
    createTenant <- jenkinsClient.createTenant("engage", "2 days", "dev")(JenkinsClient.stopOnNonSuccessfulBuild)
    tenant = createTenant.description
    arcadeStack <- jenkinsClient.deployStack(tenant, "arcade")(JenkinsClient.continueOnNonSuccessfulBuild)
    engineStack <- jenkinsClient.deployStack(tenant, "engine")(JenkinsClient.continueOnNonSuccessfulBuild)
    engageStack <- jenkinsClient.deployStack(tenant, "engage")(JenkinsClient.continueOnNonSuccessfulBuild)
    rewardsDhpUi <- jenkinsClient.deployComponent("rewards-dhp-ui", "8.8.2-2-683689a-SNAPSHOT", tenant)
  } yield {
    println(s"arcadeStack: $arcadeStack")
    println(s"engineStack: $engineStack")
    println(s"engageStack: $engageStack")
    println(s"rewardsDhpUi: $rewardsDhpUi")
    Done.done
  }
}
