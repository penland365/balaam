package codes.penland365
package balaam.clients

import codes.penland365.balaam.domain.LatLong
import codes.penland365.balaam.errors.{BadDarkSkyRequest, DarkSkyJsonDecodingFailure,
  DarkSkyResourceNotFound, InvalidDarkSkyApiKey, UnknownDarkSkyResponse}
import com.twitter.finagle.http.{Request, RequestBuilder, Status}
import com.twitter.finagle.{Addr, Address, Http, Name, Service}
import com.twitter.io.Buf
import com.twitter.logging.{Level, Logger}
import com.twitter.util.{Duration, Future, Var}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import java.net.URL
import org.jboss.netty.handler.codec.http.HttpHeaders
import io.circe.parser._

object DarkSky {
  private val log         = Logger.get("codes.penland365.balaam.clients.DarkSky")
  private val host        = "api.darksky.net"
  private val port        = 443
  private val address     = Address(host, port)
  private lazy val apiKey = ""

  private val httpClient = Http.client
    .withTls(host)
    .withRequestTimeout(Duration.fromMilliseconds(2000L))
    .newService(
      Name.Bound(Var[Addr](Addr.Bound(address)), s"$host:$port"),
      host
    )

  val GetForecast: Service[LatLong, Forecast] = new Service[LatLong, Forecast] {
    override def apply(request: LatLong): Future[Forecast] = {
      val httpRequest = buildRequest(request)
      httpClient(httpRequest) flatMap { response =>
        response.status match {
          case Status.Ok         => {
            log(Level.DEBUG, "GET darksky/forecast Response %s", response)
            val Buf.Utf8(body) = response.content
            decode[Forecast](body) match {
              case Left(error)     => Future.exception(new DarkSkyJsonDecodingFailure(error, body))
              case Right(forecast) => Future.value(forecast)
            }
          }
          case Status.BadRequest => {
            val Buf.Utf8(body) = response.content
            Future.exception(BadDarkSkyRequest(body))
          }
          case Status.Forbidden  => Future.exception(new InvalidDarkSkyApiKey(apiKey))
          case Status.NotFound   => Future.exception(new DarkSkyResourceNotFound(httpRequest))
          case unknownStatus     => Future.exception(new UnknownDarkSkyResponse(response))
        }
      }
    }
  }

  def buildRequest(latLong: LatLong): Request = RequestBuilder()
    .setHeader(HttpHeaders.Names.USER_AGENT, "balaam/v0.0.1-M1")
    .setHeader(HttpHeaders.Names.ACCEPT, "application/json")
    .url(buildForecastUrl(apiKey, latLong))
    .buildGet()

  private[clients] def buildForecastUrl(apiKey: String, latLong: LatLong): URL =
    new URL(s"https://api.darksky.net/forecast/$apiKey/${latLong.latitude},${latLong.longitude}?exclude=minutely,hourly,daily,alerts,flags")


  case class Forecast(latitude: Double, longitude: Double, timezone: String, currently: DataPoint)
  object Forecast {
    implicit val encodeForecast: Encoder[Forecast] = deriveEncoder[Forecast]
    implicit val decodeForecast: Decoder[Forecast] = deriveDecoder[Forecast]
  }

  /** A Weather DataPoint as defiend by DarkSky's API
   *
   *  For more information, see [[https://darksky.net/dev/docs#data-point]]
   */
  case class DataPoint(apparentTemperature: Option[Double], apparentTemperatureHigh: Option[Double],
    apparentTemperatureHighTime: Option[Long], apparentTemperatureLow: Option[Double],
    apparentTemperatureLowTime: Option[Long], apparentTemperatureMax: Option[Double],
    apparentTemperatureMaxTime: Option[Long], apparentTemperatureMin: Option[Double],
    apparentTemperatureMinTime: Option[Long], cloudCover: Option[Double], dewPoint: Option[Double],
    humidity: Option[Double], icon: Option[String], moonPhase: Option[String], nearestStormBearing: Option[Int],
    nearestStormDistance: Option[Int], ozone: Option[Double], precipAccumulation: Option[Double],
    precipIntensity: Option[Double], precipIntensityMax: Option[Double], precipIntensityMaxTime: Option[Long],
    precipProbability: Option[Double], precipType: Option[String], pressure: Option[Double], summary: Option[String],
    sunriseTime: Option[Long], sunsetTime: Option[Long], temperature: Option[Double], temperatureHigh: Option[Double],
    temperatureHighTime: Option[Long], temperatureLow: Option[Double], temperatureLowTime: Option[Long],
    time: Long, uvIndex: Option[Int], unIndexTime: Option[Long], visibility: Option[Int], windBearing: Option[Int],
    windGust: Option[Double], windSpeed: Option[Double])
  object DataPoint {
    implicit val encodeDataPoint: Encoder[DataPoint] = deriveEncoder[DataPoint]
    implicit val decodeDataPoint: Decoder[DataPoint] = deriveDecoder[DataPoint]
  }
}
