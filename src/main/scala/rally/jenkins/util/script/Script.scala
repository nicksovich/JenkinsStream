package rally.jenkins.util.script

import akka.Done

import scala.concurrent.Future

trait Script {
  def run: Future[Done]
}
