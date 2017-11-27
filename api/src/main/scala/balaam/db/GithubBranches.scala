package codes.penland365
package balaam.db

import codes.penland365.balaam.errors.{GithubBranchExists, GithubBranchNotFound, PostgresError}
import codes.penland365.balaam.requests
import codes.penland365.balaam.requests.{GithubBranchRequest, PutGithubBranch}
import com.twitter.finagle.Service
import com.twitter.util.Future
import com.twitter.util.logging.Logging
import java.time.ZonedDateTime
import roc.postgresql.failures.PostgresqlServerFailure
import roc.postgresql.{Request, Result, Row}
import roc.types.decoders._

case class GithubBranch(id: Int, user: User, owner: String, repo: String, branch: String,
  lastModifiedAt: ZonedDateTime, insertedAt: ZonedDateTime)
object GithubBranch {
  implicit val githubBranchDecoder: Postgresql.Decoder[GithubBranch] = new Postgresql.Decoder[GithubBranch] {
    override def decode(result: Result, exceptionMessage: String): Future[GithubBranch] = if(result.isEmpty) {
      Future.exception(new GithubBranchNotFound(exceptionMessage))
    } else {
      Future.value(result.map(row2GithubBranch).head)
    }
  }

  private val row2GithubBranch: (Row) => GithubBranch = (r: Row) => {
    val id             = r.get('github_branch_id).as[Int]
    val user           = Postgresql.row2User(r)
    val owner          = r.get('github_branch_owner).as[String]
    val repo           = r.get('github_branch_repo).as[String]
    val branch         = r.get('github_branch_branch).as[String]
    val lastModifiedAt = r.get('github_branch_last_modified_at).as[ZonedDateTime]
    val insertedAt     = r.get('github_branch_inserted_at).as[ZonedDateTime]
    new GithubBranch(id = id, user = user, owner = owner, repo = repo, branch = branch,
      lastModifiedAt = lastModifiedAt, insertedAt = insertedAt)
  }
}

object GithubBranches extends Logging {

  val insert: Service[GithubBranchInsert, Int] = new Service[GithubBranchInsert, Int] {
    override def apply(request: GithubBranchInsert): Future[Int] = {
      val sql = request.insertRequest
      trace(sql)
      Postgresql.client.query(sql).map(result => {
        if(result.isEmpty) throw new PostgresError(s"ERROR - no id returned on github branch creation")
        else result.head.get('id).as[Int]
      })
    } rescue {
        case e: PostgresqlServerFailure => e.code match {
          case x if x == "23505" => Future.exception(new GithubBranchExists(s"ERROR - $request exists")) // unique_violation
        }
      }
  }

  val update: Service[GithubBranchUpdate, String] = new Service[GithubBranchUpdate, String] {
    override def apply(request: GithubBranchUpdate): Future[String] = {
      val sql = request.updateRequest
      trace(sql)
      Postgresql.client.query(sql).map(result => result.completedCommand)
    }
  }

  val selectByIdAndUserId: Service[requests.GithubBranch, GithubBranch] =
    new Service[requests.GithubBranch, GithubBranch] {
      private val postgres = new Postgresql.Select[GithubBranch]()
      override def apply(request: requests.GithubBranch): Future[GithubBranch] = {
        val sql = Request(buildSql(request.user, request.branch))
        trace(sql)
        postgres(sql)
      }

      private def buildSql(userId: Int, branchId: Int): String =
        s"""
          SELECT
            balaam.github_branches.id AS github_branch_id,
            balaam.github_branches.owner AS github_branch_owner,
            balaam.github_branches.repo AS github_Branch_repo,
            balaam.github_branches.branch AS github_branch_branch,
            balaam.github_branches.last_modified_at AS github_branch_last_modified_at,
            balaam.github_branches.inserted_at AS github_branch_inserted_at,
            balaam.users.id AS user_id,
            balaam.users.username AS user_username,
            balaam.users.github_access_token AS user_github_access_token,
            balaam.users.last_modified_at AS user_last_modified_at,
            balaam.users.inserted_at AS user_inserted_at
          FROM balaam.github_branches
          LEFT JOIN balaam.users ON users.id = github_branches.user_id
          WHERE github_branches.id = $branchId AND github_branches.user_id = $userId;
        """
    }

  val selectByUserId: Service[Int, GithubBranch] = new Service[Int, GithubBranch] {
    private val postgres = new Postgresql.Select[GithubBranch]()

    override def apply(id: Int): Future[GithubBranch] = {
      val sql = Request(buildSql(id))
      postgres.errorMessage_=(s"ERROR - no Github Branch found for user id $id")
      trace(sql)
      postgres(sql)
    }

    private def buildSql(userId: Int): String =
      s"""
        SELECT
          balaam.github_branches.id AS github_branch_id,
          balaam.github_branches.owner AS github_branch_owner,
          balaam.github_branches.repo AS github_Branch_repo,
          balaam.github_branches.branch AS github_branch_branch,
          balaam.github_branches.last_modified_at AS github_branch_last_modified_at,
          balaam.github_branches.inserted_at AS github_branch_inserted_at,
          balaam.users.id AS user_id,
          balaam.users.username AS user_username,
          balaam.users.github_access_token AS user_github_access_token,
          balaam.users.last_modified_at AS user_last_modified_at,
          balaam.users.inserted_at AS user_inserted_at
        FROM balaam.github_branches
        LEFT JOIN balaam.users ON users.id = github_branches.user_id
        WHERE github_branches.user_id = $userId;
      """
  }
}

case class GithubBranchInsert(userId: Int, owner: String, repo: String, branch: String) {
  def insertRequest: Request = Request(s"""
    INSERT INTO balaam.github_branches
    (user_id, owner, repo, branch)
    VALUES
    ($userId, '$owner', '$repo', '$branch')
    RETURNING id;
   """)
}
object GithubBranchInsert {
  def apply(user: User, request: GithubBranchRequest): GithubBranchInsert =
    new GithubBranchInsert(userId = user.id,
                           owner  = request.owner,
                           repo   = request.repo,
                           branch = request.branch)

}

case class GithubBranchUpdate(id: Int, userId: Int, owner: String, repo: String, branch: String) {
  def updateRequest: Request = Request(s"""
    UPDATE balaam.github_branches
    SET
      owner  = '$owner',
      repo   = '$repo',
      branch = '$branch'
    WHERE id = $id AND user_id = $userId;
  """)
}
object GithubBranchUpdate {
  def apply(user: User, request: PutGithubBranch): GithubBranchUpdate =
    new GithubBranchUpdate(id     = request.id,
                           userId = user.id,
                           owner  = request.owner,
                           repo   = request.repo,
                           branch = request.branch)
}
