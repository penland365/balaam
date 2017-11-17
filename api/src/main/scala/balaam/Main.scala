package codes.penland365
package balaam

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.filters.{CommonFilters, LoggingMDCFilter, TraceIdMDCFilter}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.routing.HttpRouter

object Main extends BalaamServer

class BalaamServer extends HttpServer {
  override val defaultFinatraHttpPort: String = ":8080"

  override def configureHttp(router: HttpRouter): Unit = {
    val _ = router
    .filter[LoggingMDCFilter[Request, Response]]
    .filter[TraceIdMDCFilter[Request, Response]]
    .filter[CommonFilters]
    .add[DataController]
  }
}
