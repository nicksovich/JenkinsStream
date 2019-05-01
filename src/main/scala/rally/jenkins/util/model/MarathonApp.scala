package rally.jenkins.util.model

case class MarathonApp(
  id: String,
  env: Map[String, String],
  tasksStaged: Int,
  tasksRunning: Int,
  tasksHealthy: Int,
  tasksUnhealthy: Int
)
