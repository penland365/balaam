package codes.penland365
package balaam.clients

import com.twitter.finagle.http.{Request, RequestBuilder, Status}
import codes.penland365.balaam.errors.{GithubJsonDecodingFailure, UnknownGithubResponse}
import com.twitter.finagle.{Addr, Address, Http, Name, Service}
import com.twitter.io.Buf
import com.twitter.util.logging.Logging
import com.twitter.util.{Duration, Future, Var}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser._
import io.circe.{Decoder, Encoder}
import java.net.URL
import org.jboss.netty.handler.codec.http.HttpHeaders

object Github extends Logging {
  private val host    = "api.github.com"
  private val port    = 443
  private val address = Address(host, port)

  private val httpClient = Http.client
    .withTls(host)
    .withRequestTimeout(Duration.fromMilliseconds(3000L))
    .newService(
      Name.Bound(Var[Addr](Addr.Bound(address)), s"$host:$port"),
      host
    )

  val GetNotifications: Service[String, List[Notification]] = new Service[String, List[Notification]] {
    override def apply(token: String): Future[List[Notification]] = {
      val request = buildRequest(token)
      httpClient(request) flatMap { response =>
        response.status match {
          case Status.Ok         => {
            trace(s"GET api.github.com/notifications $response")
            val Buf.Utf8(body) = response.content
            decode[List[Notification]](body) match {
              case Left(error)          => Future.exception(new GithubJsonDecodingFailure(error, body))
              case Right(notifications) => Future.value(notifications)
            }
          }
          case unknownStatus     => Future.exception(new UnknownGithubResponse(response))
        }
      }
    }
  }

  case class ListBranchesReq(token: String, org: String, repo: String)
  val ListBranches: Service[ListBranchesReq, List[Branch]] = new Service[ListBranchesReq, List[Branch]] {

    override def apply(request: ListBranchesReq): Future[List[Branch]] = {
      val httpReq = buildListBranchesReq(request)
      httpClient(httpReq) flatMap { response =>
        response.status match {
          case Status.Ok     => {
            trace(s"GET api.github.com/list-notifications $response")
            val Buf.Utf8(body) = response.content
            decode[List[Branch]](body) match {
              case Left(error)     => Future.exception(new GithubJsonDecodingFailure(error, body))
              case Right(branches) => Future.value(branches)
            }
          }
          case unknownStatus => Future.exception(new UnknownGithubResponse(response))
        }
      }
    }
  }

  case class GetBranchStatusReq(token: String, org: String, repo: String, sha: String)
  val GetBranchStatus: Service[GetBranchStatusReq, BranchStatus] = new Service[GetBranchStatusReq, BranchStatus] {

    override def apply(req: GetBranchStatusReq): Future[BranchStatus] = {
      val httpReq = buildGetBranchStatusRequest(req)
      httpClient(httpReq) flatMap { response =>
        response.status match {
          case Status.Ok     => {
            trace(s"GET api.github.com/status $response")
            val Buf.Utf8(body) = response.content
            decode[BranchStatus](body) match {
              case Left(error)   => Future.exception(new GithubJsonDecodingFailure(error, body))
              case Right(status) => Future.value(status)
            }
          }
          case unknownStatus => Future.exception(new UnknownGithubResponse(response))
        }
      }
    }
  }

  def buildRequest(token: String): Request = RequestBuilder()
    .setHeader(HttpHeaders.Names.USER_AGENT, "balaam/v0.3.0-M6")
    .setHeader(HttpHeaders.Names.ACCEPT, "application/vnd.github.v3+json")
    .setHeader(HttpHeaders.Names.AUTHORIZATION, s"token $token")
    .url(new URL("https://api.github.com/notifications"))
    .buildGet()

  def buildListBranchesReq(req: ListBranchesReq): Request = RequestBuilder()
    .setHeader(HttpHeaders.Names.USER_AGENT, "balaam/v0.3.0-M6")
    .setHeader(HttpHeaders.Names.ACCEPT, "application/vnd.github.v3+json")
    .setHeader(HttpHeaders.Names.AUTHORIZATION, s"token ${req.token}")
    .url(new URL(s"https://api.github.com/repos/${req.org}/${req.repo}/branches"))
    .buildGet()

  def buildGetBranchStatusRequest(req: GetBranchStatusReq): Request = RequestBuilder()
    .setHeader(HttpHeaders.Names.USER_AGENT, "balaam/v0.3.0-M6")
    .setHeader(HttpHeaders.Names.ACCEPT, "application/vnd.github.v3+json")
    .setHeader(HttpHeaders.Names.AUTHORIZATION, s"token ${req.token}")
    .url(new URL(s"https://api.github.com/repos/${req.org}/${req.repo}/commits/${req.sha}/status"))

    .buildGet()

  case class Notification(id: String, reason: String, unread: Boolean)
  object Notification {
    implicit val encodeNotification: Encoder[Notification] = deriveEncoder[Notification]
    implicit val decodeNotification: Decoder[Notification] = deriveDecoder[Notification]
  }

  case class Commit(sha: String, url: String)
  object Commit {
    implicit val encodeCommit: Encoder[Commit] = deriveEncoder[Commit]
    implicit val decodeCommit: Decoder[Commit] = deriveDecoder[Commit]
  }

  case class Branch(name: String, commit: Commit)
  object Branch {
    implicit val encodeBranch: Encoder[Branch] = deriveEncoder[Branch]
    implicit val decodeBranch: Decoder[Branch] = deriveDecoder[Branch]
  }

  case class BranchStatus(state: String, sha: String)
  object BranchStatus {
    implicit val encodeBranchStatus: Encoder[BranchStatus] = deriveEncoder[BranchStatus]
    implicit val decodeBranchStatus: Decoder[BranchStatus] = deriveDecoder[BranchStatus]
  }
}
