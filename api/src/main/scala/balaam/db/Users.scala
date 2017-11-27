package codes.penland365
package balaam.db

import codes.penland365.balaam.errors.BalaamUserNotFound
import codes.penland365.balaam.requests.CreateUserRequest
import com.twitter.finagle.Service
import com.twitter.util.Future
import com.twitter.util.logging.Logging
import java.time.ZonedDateTime
import roc.postgresql.{Request, Result, Row}
import roc.types.decoders._

case class User(id: Int, username: String, githubAccessToken: Option[String],
  lastModifiedAt: ZonedDateTime, insertedAt: ZonedDateTime) {

  //def fromUpdatedBranch(newBranch: Option[String]): User =
  //  new User(id = id, username = username, githubAccessToken = githubAccessToken, githubBranch = newBranch,
  //    lastModifiedAt = lastModifiedAt, insertedAt = insertedAt)
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
    val lastModifiedAt    = r.get('last_modified_at).as[ZonedDateTime]
    val insertedAt        = r.get('inserted_at).as[ZonedDateTime]
    new User(id = id, username = username, githubAccessToken = githubAccessToken,
      lastModifiedAt = lastModifiedAt, insertedAt = insertedAt)
  }
}

object Users extends Logging {

  val selectById: Service[Int, User] = new Service[Int, User] {
    private val postgres = new Postgresql.Select[User]()

    override def apply(id: Int): Future[User] = {
      val sql = Request(s"SELECT * FROM balaam.users WHERE id = $id;")
      postgres.errorMessage_=(s"ERROR - no balaam user found for id $id.")
      postgres(sql)
    }
  }

  val insert: Service[InsertUser, Int] = new Service[InsertUser, Int] {

    override def apply(request: InsertUser): Future[Int] = {
      val sql = request.insertSql
      trace(sql)
      Postgresql.client.query(sql).map(result => {
        result.head.get('id).as[Int]
      })
    }
  }

  val deleteById: Service[Int, String] = new Service[Int, String] {
    override def apply(id: Int): Future[String] = {
      val sql = Request(s"DELETE FROM balaam.users WHERE id = $id;")
      trace(sql)
      Postgresql.client.query(sql).map(_.completedCommand)
    }
  }

  //val updateBranch: Service[User, String] = new Service[User, String] {
  //  override def apply(user: User): Future[String] = {
  //    val branchSql = user.githubBranch match {
  //      case Some(x) => s"'$x'"
  //      case None    => "DEFAULT"
  //    }
  //    val sql = Request(s"UPDATE balaam.users SET github_branch = $branchSql WHERE id = ${user.id};")
  //    Postgresql.client.query(sql).map(_.completedCommand)
  //  }
  //}
}

case class InsertUser(username: String, githubAccessToken: Option[String]) {
  def insertSql: Request = {
    val gatSql = githubAccessToken match {
      case None    => "DEFAULT"
      case Some(x) => s"""'$x'"""
    }
    Request(s"INSERT INTO balaam.users (username, github_access_token) VALUES ('$username', $gatSql) RETURNING id;")
  }
}

object InsertUser {
  def apply(request: CreateUserRequest): InsertUser =
    new InsertUser(request.username, request.githubAccessToken)
}
