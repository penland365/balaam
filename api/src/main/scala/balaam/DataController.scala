package codes.penland365
package balaam

import codes.penland365.balaam.requests.{GithubBranchRequest, GithubRequest}
import com.twitter.finatra.http.Controller
import com.twitter.finatra.request.RouteParam

final class DataController extends Controller {
  import DataController.WeatherRequest

  prefix("/data") {
    get("/weather/:latitude/:longitude") { request: WeatherRequest =>
      services.GetWeather(request).flatMap(domain.EncodeWeather)
    }
    get("/github/:id/notifications") { request: GithubRequest =>
      services.ListNotifications(request.id).map(_.size)
    }
    get("/github/:id/branch") { request: GithubRequest =>
      services.GetBranch(request.id).map(_.githubBranch.getOrElse(""))
    }
    post("/github/:id/branch") { request: GithubBranchRequest =>
      services.UpdateBranch(request).map(_ => response.created)
    }
  }
}

object DataController {
  case class WeatherRequest(@RouteParam latitude: Double, @RouteParam longitude: Double)
}
