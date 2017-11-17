package codes.penland365
package balaam

import com.twitter.finatra.http.Controller
import com.twitter.finatra.request.RouteParam

final class DataController extends Controller {
  import DataController.{GithubRequest, WeatherRequest}

  prefix("/data") {
    get("/weather/:latitude/:longitude") { request: WeatherRequest =>
      services.GetWeather(request).flatMap(domain.EncodeWeather)
    }
    get("/github/:token/notifications") { request: GithubRequest =>
      services.ListNotifications(request.token).map(_.size)
    }
  }
}

object DataController {
  case class WeatherRequest(@RouteParam latitude: Double, @RouteParam longitude: Double)
  case class GithubRequest(@RouteParam token: String)
}
