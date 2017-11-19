package codes.penland365
package balaam

import com.twitter.finatra.request.RouteParam

object requests {
  case class GithubBranchRequest(@RouteParam id: Int)
  case class GithubRequest(@RouteParam id: Int)
}
