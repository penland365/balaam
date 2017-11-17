package codes.penland365
package balaam

import com.twitter.finagle.Service
import com.twitter.util.Future

object domain {
  case class LatLong(latitude: Double, longitude: Double)

  case class Weather(temp: Double, icon: String)
  val EncodeWeather: Service[Weather, String] = new Service[Weather, String] {
    override def apply(weather: Weather): Future[String] = {
      Future.value(s"${weather.temp} ${weather.icon}")
    }
  }
}
