# Rally Util
A repo containing useful Rally utility libraries. 

## JenkinsStream
Jenkins has a REST api which this module wraps. 

job().onComplete {
    case Success(JobInfo(buildId, result)) => result match {
        case Aborted() => ???
        case Success() => nextJob().onComplete {???}
        case Fail() => ???
    } 
    case Failure(ex) => ???
}

