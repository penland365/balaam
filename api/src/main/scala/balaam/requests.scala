package codes.penland365
package balaam

import com.twitter.finatra.request.RouteParam
import com.twitter.finatra.validation.NotEmpty

object requests {
  case class GithubBranchRequest(@RouteParam id: Int, @NotEmpty branch: String)
  case class GithubRequest(@RouteParam id: Int)
}
