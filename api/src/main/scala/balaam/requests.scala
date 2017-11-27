package codes.penland365
package balaam

import com.twitter.finatra.request.RouteParam
import com.twitter.finatra.validation.NotEmpty

object requests {
  case class GithubBranchRequest(@RouteParam id: Int, @NotEmpty owner: String,
    @NotEmpty repo: String ,@NotEmpty branch: String)
  case class GithubRequest(@RouteParam id: Int)
  case class WeatherRequest(@RouteParam latitude: Double, @RouteParam longitude: Double)
  case class CreateUserRequest(@NotEmpty username: String, githubAccessToken: Option[String])

  case class DeleteUserRequest(@RouteParam id: Int)

  case class PutGithubBranch(@RouteParam user: Int,
                             @RouteParam id: Int,
                             @NotEmpty owner: String,
                             @NotEmpty repo: String,
                             @NotEmpty branch: String)
  case class GithubBranch(@RouteParam user: Int, @RouteParam branch: Int)
}
