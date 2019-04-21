package rally.jenkins.util

/**
  *
  * @param baseURL ex: "https://ci.rally-dev.com"
  * @param username ex: "first.last@rallyhealth.com"
  * @param token Jenkins API token that you can find in your Jenkins settings page
  */
case class JenkinsConfig(baseURL: String, username: String, token: String)
