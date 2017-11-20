package codes.penland365
package balaam.db

import com.twitter.finagle.Service
import codes.penland365.balaam.errors.BalaamUserNotFound
import com.twitter.util.Future
import roc.postgresql.{Request, Result, Row}
import roc.types.decoders._
import java.time.ZonedDateTime

case class User(id: Int, username: String, githubAccessToken: Option[String],
  githubBranch: Option[String], lastModifiedAt: ZonedDateTime, insertedAt: ZonedDateTime) {

  def fromUpdatedBranch(newBranch: Option[String]): User =
    new User(id = id, username = username, githubAccessToken = githubAccessToken, githubBranch = newBranch,
      lastModifiedAt = lastModifiedAt, insertedAt = insertedAt)
}

private[db] object User {
  implicit val userDecoder: Postgresql.Decoder[User] = new Postgresql.Decoder[User] {
    override def decode(result: Result, exceptionMessage: String): Future[User] = if(result.isEmpty) {
      Future.exception(new BalaamUserNotFound(exceptionMessage))
    } else {
      Future.value(result.map(row2User).head)
    }
  }

  private[db] def row2User(r: Row): User = {
    val id                = r.get('id).as[Int]
    val username          = r.get('username).as[String]
    val githubAccessToken = r.get('github_access_token).as[Option[String]]
    val githubBranch      = r.get('github_branch).as[Option[String]]
    val lastModifiedAt    = r.get('last_modified_at).as[ZonedDateTime]
    val insertedAt        = r.get('inserted_at).as[ZonedDateTime]
    new User(id = id, username = username, githubAccessToken = githubAccessToken,
      githubBranch = githubBranch, lastModifiedAt = lastModifiedAt, insertedAt = insertedAt)
  }
}

object Users {

  val selectById: Service[Int, User] = new Service[Int, User] {
    private val postgres = new Postgresql.Select[User]()

    override def apply(id: Int): Future[User] = {
      val sql = Request(s"SELECT * FROM balaam.users WHERE id = $id;")
      postgres.errorMessage_=(s"ERROR - no balaam user found for id $id.")
      postgres(sql)
    }
  }

  val updateBranch: Service[User, String] = new Service[User, String] {
    override def apply(user: User): Future[String] = {
      val branchSql = user.githubBranch match {
        case Some(x) => s"'$x'"
        case None    => "DEFAULT"
      }
      val sql = Request(s"UPDATE balaam.users SET github_branch = $branchSql WHERE id = ${user.id};")
      Postgresql.client.query(sql).map(_.completedCommand)
    }
  }
}
