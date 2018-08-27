package codes.penland365
package balaam

import codes.penland365.balaam.requests._
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller

final class DataController extends Controller {

  post("/users") { request: CreateUserRequest =>
    services.CreateUser(request).map(id => response.created.body(id).location(s"/users/$id"))
  }
  prefix("/user") {
    delete("/:id") { request: DeleteUserRequest =>
      services.DeleteUser(request).map(_ => response.noContent)
    }
    post("/:id/branches") { request: GithubBranchRequest =>
      services.CreateBranch(request).map(id => response.created.body(id).location(s"/user/${request.id}/branch/$id"))
    }
    put("/:user/branch/:id") { request: PutGithubBranch =>
      services.UpdateBranch(request).map(completed => response.ok.body(completed))
    }
    get("/:id/github/branch") { request: GithubRequest =>
      services.GetBranch(request.id).map(branch => response.ok.body(branch.branch))
    }
    get("/:id/github/notifications") { request: GithubRequest =>
      services.ListNotifications(request.id).map(_.size)
    }
    get("/:id/github/branch/status") { request: GithubRequest =>
      services.GetBranchStatus(request)
    }
  }
  get("/data/weather/:latitude/:longitude") { request: WeatherRequest =>
    services.GetWeather(request).flatMap(domain.EncodeWeather)
  }

  get("/admin/health") { _: Request =>
    "OK"
  }
  get("/admin/ping") { _: Request =>
    "pong"
  }
}
