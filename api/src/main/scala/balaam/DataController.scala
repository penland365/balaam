package codes.penland365
package balaam

import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller

final class DataController extends Controller {

  val encoder = domain.EncodeWeather
  prefix("/data") {
    get("/weather") { request: Request =>
      val latLong = domain.LatLong(32.7894259,-96.8046665)
      services.GetWeather(latLong).flatMap(domain.EncodeWeather)
    }
  }
}
