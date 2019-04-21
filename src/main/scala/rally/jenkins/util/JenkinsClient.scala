package rally.jenkins.util

import rally.jenkins.util.model.JobInfo

import scala.concurrent.Future

trait JenkinsClient {

  type QueueId = String
  type Status = String
  type Error = Int

  def createTenant(stacks: String, lifespan: String, environment: String): Future[Either[QueueId, Error]]

  def destroyTenant(tenant: String): Future[Either[QueueId, Error]]

  def deployStack(): Future[QueueId]

  def deployComponent(component: String, version: String, tenant: String): Future[Either[QueueId, Error]]

  def waitForJobToFinish(queueId: QueueId): Future[Either[Status, Error]]

  def getLastJobInfo(jobName: String): Future[JobInfo]

  def getLastNJobInfos(jobName: String, n: Int): Future[Seq[JobInfo]]

  def getJobInfo(jobName: String, buildNumber: Int): Future[JobInfo]

  def getJobsInfos(jobName: String, startBuildNumber: Int, endBuildNumber: Int): Future[Seq[JobInfo]]
}
