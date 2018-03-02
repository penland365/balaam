package codes.penland365
package balaam

import codes.penland365.balaam.clients.Github.Notification
import codes.penland365.balaam.clients.{DarkSky, Github}
import codes.penland365.balaam.db.{InsertUser, Users}
import codes.penland365.balaam.domain._
import codes.penland365.balaam.requests._
import com.twitter.finagle.Service
import com.twitter.storehaus.cache.MutableTTLCache
import com.twitter.util.logging.Logging
import com.twitter.util.{Duration, Future}

object services extends Logging {

  val CreateUser: Service[CreateUserRequest, Int] = new Service[CreateUserRequest, Int] {
    override def apply(request: CreateUserRequest): Future[Int] =
      Users.insert(InsertUser(request))
  }

  val DeleteUser: Service[DeleteUserRequest, Unit] = new Service[DeleteUserRequest, Unit] {
    override def apply(request: DeleteUserRequest): Future[Unit] = for {
      user <- Users.selectById(request.id)
      _    <- Users.deleteById(user.id)
    } yield ()
  }

  val GetWeather: Service[WeatherRequest, Weather] = new Service[WeatherRequest, Weather] {
    private val cache = MutableTTLCache[Int, DarkSky.Forecast](Duration.fromSeconds(90), 7)
    override def apply(request: WeatherRequest): Future[Weather] = cache.get(request.hashCode) match {
      case Some(x) => Future.value(forecastToWeather(x))
      case None    => {
        debug(s"Fetching new Weather for $request")
        DarkSky.GetForecast(request) map { forecast =>
          cache += ((request.hashCode, forecast))
          forecastToWeather(forecast)
        }
      }
    }

    private[services] def forecastToWeather(forecast: DarkSky.Forecast): Weather = {
      val temp = forecast.currently.apparentTemperature.getOrElse(-459.67)
      val icon = forecast.currently.emojiForIcon
      new Weather(temp, icon)
    }
  }

  val ListNotifications: Service[Int, List[Notification]] = new Service[Int, List[Notification]] {
    private val cache = MutableTTLCache[Int, List[Notification]](Duration.fromSeconds(20), 7)

    override def apply(id: Int): Future[List[Notification]] = cache.get(id) match {
      case Some(x) => Future.value(x)
      case None    => for {
        user          <- db.Users.selectById(id)
        _             =  debug(s"Fetching new Github Notifications for $user")
        notifications <- Github.GetNotifications(user.githubAccessToken.getOrElse(""))
      } yield {
        cache += ((id, notifications))
        notifications
      }
    }
  }

  val GetBranch: Service[Int, db.GithubBranch] = new Service[Int, db.GithubBranch] {
    override def apply(id: Int): Future[db.GithubBranch] = db.GithubBranches.selectByUserId(id)
  }

  val UpdateBranch: Service[PutGithubBranch, String] = new Service[PutGithubBranch, String] {
    override def apply(request: PutGithubBranch): Future[String] = for {
      user      <- db.Users.selectById(request.user)
      update    =  db.GithubBranchUpdate(user, request)
      completed <- db.GithubBranches.update(update)
      _         =  info(s"GithubBranchUpdate[$update] completed command $completed")
    } yield completed
  }

  val CreateBranch: Service[GithubBranchRequest, Int] = new Service[GithubBranchRequest, Int] {
    override def apply(request: GithubBranchRequest): Future[Int] = for {
      user   <- db.Users.selectById(request.id)
      insert =  db.GithubBranchInsert(user, request)
      id     <- db.GithubBranches.insert(insert)
    } yield id
  }

  val GetBranchStatus: Service[GithubRequest, String] = new Service[GithubRequest, String] {
    private val cache = MutableTTLCache[Int, String](Duration.fromSeconds(10), 7)

    override def apply(request: GithubRequest): Future[String] = cache.get(request.hashCode) match {
      case Some(x) => Future.value(x)
      case None    => for {
        user     <- db.Users.selectById(request.id)
        branch   <- db.GithubBranches.selectByUserId(request.id)
        branches <- Github.ListBranches(Github.ListBranchesReq(user.githubAccessToken.get, branch.owner, branch.repo))
        status   <- parseSha(Some(branch.branch), branches) match {
          case None      => Future.value(new Github.BranchStatus("ERROR", ""))
          case Some(sha) => Github.GetBranchStatus(Github.GetBranchStatusReq(user.githubAccessToken.get,
                                                                             branch.owner,
                                                                             branch.repo,
                                                                             sha))
        }
      } yield {
        cache += ((request.hashCode, status.state))
        status.state
      }
    }

    private def parseSha(branchName: Option[String], branches: List[Github.Branch]): Option[String] = branchName match {
      case None    => None
      case Some(x) => {
        val branch = branches.filter(y => y.name == x)
        if(branch.isEmpty) None else Some(branch.head.commit.sha)
      }
    }
  }
}
