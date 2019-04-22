package rally.jenkins.util.model

import rally.jenkins.util.enum.{BuildResult, JobName}

/**
  *
  * @param buildId            build number of job
  * @param result            "SUCCESS" | "FAILURE" | "ABORTED"
  * @param description       text specific to the job. Only meaningful for CreateTenant, as it represents the tenant name
  */

case class BuildInfo(
  jobName: JobName,
  buildId: Int,
  result: BuildResult,
  description: String
)
