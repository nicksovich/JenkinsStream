package rally.jenkins.util.model

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

/**
  *
  * @param number            build number of job
  * @param queueId           the queueId that the job had when it was in the queue
  * @param result            "SUCCESS" | "FAILURE" | "ABORTED"
  * @param description       text specific to the job. Only meaningful for CreateTenant, as it represents the tenant name
  * @param duration          time the job took to execute
  * @param estimatedDuration time the job was expected to take to execute
  */

case class RawBuildInfo(
  description: Option[String],
  duration: Int,
  estimatedDuration: Int,
  number: Int,
  queueId: Int,
  result: Option[String]
)
