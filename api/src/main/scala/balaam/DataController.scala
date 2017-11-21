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
    prefix("/github") {
      get("/:id/notifications") { request: GithubRequest =>
        services.ListNotifications(request.id).map(_.size)
      }
      get("/:id/branch") { request: GithubRequest =>
        services.GetBranch(request.id).map(_.githubBranch.getOrElse(""))
      }
      post("/:id/branch") { request: GithubBranchRequest =>
        services.UpdateBranch(request).map(_ => response.created)
      }
      get("/:id/branch/status") { request: GithubRequest =>
        services.GetBranchStatus(request)
      }
    }
  }
}

object DataController {
  case class WeatherRequest(@RouteParam latitude: Double, @RouteParam longitude: Double)
}
