package codes.penland365
package balaam

import com.twitter.finagle.http.{Request, Response}
import io.circe.Error

object errors {
  sealed abstract class DarkSkyError(message: String) extends RuntimeException(message)

  final case class InvalidDarkSkyApiKey(message: String) extends DarkSkyError(message)
  final case class UnknownDarkSkyResponse(response: Response)
    extends DarkSkyError(s"ERROR - Unknown HTTP Response Code [${response.status}]")
  final case class BadDarkSkyRequest(message: String)
    extends DarkSkyError(s"ERROR - HTTP 400 with body $message")
  final case class DarkSkyResourceNotFound(request: Request)
    extends DarkSkyError(s"ERROR - HTTP 404 for resource ${request.uri}")
  final case class DarkSkyJsonDecodingFailure(error: Error, json: String)
    extends DarkSkyError(s"ERROR - Could not JSON Decode DarkSky HTTP 200, Error[$error], JSON[$json].")
}
