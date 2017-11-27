package codes.penland365
package balaam

import codes.penland365.balaam.errors.{BalaamUserNotFound, GithubBranchExists}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.exceptions.ExceptionMapper
import com.twitter.finatra.http.response.ResponseBuilder
import javax.inject.{Inject, Singleton}

@Singleton
final class BalaamUserNotFoundExceptionMapper @Inject()(response: ResponseBuilder)
  extends ExceptionMapper[BalaamUserNotFound] {
  override def toResponse(request: Request, throwable: BalaamUserNotFound): Response = {
   response.notFound(throwable.getMessage)
  }
}

@Singleton
final class GithubBranchExistsExceptionMapper @Inject()(response: ResponseBuilder)
  extends ExceptionMapper[GithubBranchExists] {
  override def toResponse(request: Request, throwable: GithubBranchExists): Response =
    response.conflict(throwable.getMessage)
}
