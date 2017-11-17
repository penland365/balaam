package codes.penland365
package balaam

import codes.penland365.balaam.clients.DarkSky
import codes.penland365.balaam.domain._
import com.twitter.finagle.Service
import com.twitter.storehaus.cache.MutableTTLCache
import com.twitter.util.logging.Logging
import com.twitter.util.{Duration, Future}
import codes.penland365.balaam.DataController.WeatherRequest

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
}
