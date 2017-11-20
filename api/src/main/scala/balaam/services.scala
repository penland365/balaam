package codes.penland365
package balaam

import codes.penland365.balaam.clients.Github.Notification
import codes.penland365.balaam.clients.{DarkSky, Github}
import codes.penland365.balaam.DataController.WeatherRequest
import codes.penland365.balaam.db.Users
import codes.penland365.balaam.domain._
import codes.penland365.balaam.requests.GithubBranchRequest
import com.twitter.finagle.Service
import com.twitter.storehaus.cache.MutableTTLCache
import com.twitter.util.logging.Logging
import com.twitter.util.{Duration, Future}

object services extends Logging {

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
    private val cache = MutableTTLCache[Int, List[Notification]](Duration.fromSeconds(60), 7)

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

  val GetBranch: Service[Int, db.User] = new Service[Int, db.User] {
    override def apply(id: Int): Future[db.User] = db.Users.selectById(id)
  }

  val UpdateBranch: Service[GithubBranchRequest, Unit] = new Service[GithubBranchRequest, Unit] {
    override def apply(request: GithubBranchRequest): Future[Unit] = for {
      user        <- db.Users.selectById(request.id)
      updatedUser =  user.fromUpdatedBranch(Some(request.branch))
      completed   <- Users.updateBranch(updatedUser)
    } yield ()
  }
}
