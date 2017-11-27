package codes.penland365
package balaam

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.filters.{CommonFilters, LoggingMDCFilter, TraceIdMDCFilter}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.app.Flag

object Main extends BalaamServer {
  val darkSkyApiKey: Flag[String]  = flag("darksky.api-key", "", "DarkSky Api Key")
  val DatabaseHost: Flag[String]   = flag("db.host", "localhost", "Database host")
  val DatabasePort: Flag[String]   = flag("db.port", ":5432", "Database Port")
  val Database: Flag[String]       = flag("db.name", "numbers", "Database")
  val DatabaseUser: Flag[String]   = flag("db.user", "moses", "Database Username")
  val DatabasePasswd: Flag[String] = flag("db.passwd", "opensesame", "Database Password")
}

class BalaamServer extends HttpServer {
  override val defaultFinatraHttpPort: String = ":8080"

  override def configureHttp(router: HttpRouter): Unit = {
    val _ = router
    .filter[LoggingMDCFilter[Request, Response]]
    .filter[TraceIdMDCFilter[Request, Response]]
    .filter[CommonFilters]
    .add[DataController]
    .exceptionMapper[BalaamUserNotFoundExceptionMapper]
    .exceptionMapper[GithubBranchExistsExceptionMapper]
  }
}
