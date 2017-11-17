package codes.penland365
package balaam

import com.twitter.finatra.http.Controller
import com.twitter.finatra.request.RouteParam

final class DataController extends Controller {
  import DataController.WeatherRequest

  prefix("/data") {
    get("/weather/:latitude/:longitude") { request: WeatherRequest =>
      services.GetWeather(request).flatMap(domain.EncodeWeather)
    }
  }
}

object DataController {
  case class WeatherRequest(@RouteParam latitude: Double, @RouteParam longitude: Double)
}
