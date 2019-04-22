package rally.jenkins.util.enum

sealed trait JobName
case object CreateTenant extends JobName {

  override def toString: String = "CreateTenant"
}

case object DeployStack extends JobName {

  override def toString: String = "DeployStack"
}

case object DeployComponent extends JobName {

  override def toString: String = "DeployComponent"
}

case object DestroyTenant extends JobName {

  override def toString: String = "DestroyTenant"
}

case object Unknown extends JobName {

  override def toString: String = "Unknown"
}

object JobName {
  val values = List(CreateTenant, DeployComponent, DeployStack, DestroyTenant)

  def fromString(string: String): JobName = values.find(j => j.toString == string).getOrElse(Unknown)
}
