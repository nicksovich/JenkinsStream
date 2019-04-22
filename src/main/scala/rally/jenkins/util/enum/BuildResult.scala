package rally.jenkins.util.enum

sealed trait BuildResult
case object BuildSuccess extends BuildResult {

  override def toString: String = "SUCCESS"
}
case object BuildFailure extends BuildResult {

  override def toString: String = "FAILURE"
}
case object BuildAborted extends BuildResult {

  override def toString: String = "ABORTED"
}
case object BuildUnknown extends BuildResult {

  override def toString: String = "Unknown"
}

object BuildResult {
  val values = List(BuildSuccess, BuildFailure, BuildAborted)
  def toBuildResult(s: String): BuildResult = values.find(r => r.toString == s).getOrElse(BuildUnknown)
}
