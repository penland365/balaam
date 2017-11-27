package codes.penland365
package balaam.db

import codes.penland365.balaam.Main
import com.twitter.finagle.Service
import com.twitter.util.Future
import java.time.ZonedDateTime
import roc.postgresql.{Request, Result, Row}
import roc.types.decoders._

private[db] object Postgresql {

  lazy val client = roc.Postgresql.client
    .withUserAndPasswd(Main.DatabaseUser(), Main.DatabasePasswd())
    .withDatabase(Main.Database())
    .newRichClient(s"${Main.DatabaseHost()}${Main.DatabasePort()}")

  trait Decoder[A] {
    def decode(result: Result, exceptionMessage: String): Future[A]
  }

  class Select[A : Decoder](private var errMsg: String = "")(implicit d: Decoder[A])
    extends Service[Request, A] {
    override def apply(request: Request): Future[A]  = for {
      result <- client.query(request)
      a      <- d.decode(result, errMsg)
    } yield a

    def errorMessage_=(newMessage: String): Unit = errMsg = newMessage
  }

  private[db] def row2User(r: Row): User = {
    val id                = r.get('user_id).as[Int]
    val username          = r.get('user_username).as[String]
    val githubAccessToken = r.get('user_github_access_token).as[Option[String]]
    val lastModifiedAt    = r.get('user_last_modified_at).as[ZonedDateTime]
    val insertedAt        = r.get('user_inserted_at).as[ZonedDateTime]
    new User(id = id, username = username, githubAccessToken = githubAccessToken,
      lastModifiedAt = lastModifiedAt, insertedAt = insertedAt)
  }
}

