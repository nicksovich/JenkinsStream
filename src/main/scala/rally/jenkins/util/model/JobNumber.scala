package rally.jenkins.util.model

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

case class JobNumber(number: Int)

case object JobNumber extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val jobNumberFormat = jsonFormat1(JobNumber.apply)
}
