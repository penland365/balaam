package codes.penland365
package balaam

import com.twitter.finagle.Service
import com.twitter.util.Future
import codes.penland365.balaam.domain._

object services {

  val GetWeather: Service[LatLong, Weather] = new Service[LatLong, Weather] {
    override def apply(request: LatLong): Future[Weather] = {
      Future.value(new Weather(71.01, "⛅"))
    }
  }
}
