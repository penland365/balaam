package codes.penland365
package balaam.clients

import org.scalatest.FunSuite

final class DataPointTests extends FunSuite {
  test("clear-day has correct emoji ☀️" ) {
    val dataPoint = buildDataPoint(Some("clear-day"))
    assert("☀️" == dataPoint.emojiForIcon)
  }

  test("clear-night has correct emoji 🌚" ) {
    val dataPoint = buildDataPoint(Some("clear-night"))
    assert("🌚" === dataPoint.emojiForIcon)
  }

  test("rain has correct emoji 🌧") {
    val dataPoint = buildDataPoint(Some("rain"))
    assert("🌧" === dataPoint.emojiForIcon)
  }

  test("snow has correct emoji ❄️") {
    val dataPoint = buildDataPoint(Some("snow"))
    assert("❄️" === dataPoint.emojiForIcon)
  }

  test("sleet has correct emoji 🌨") {
    val dataPoint = buildDataPoint(Some("sleet"))
    assert("🌨" === dataPoint.emojiForIcon)
  }

  test("wind has correct emoji 💨") {
    val dataPoint = buildDataPoint(Some("wind"))
    assert("💨" === dataPoint.emojiForIcon)
  }

  test("fog has correct emoji ☁️") {
    val dataPoint = buildDataPoint(Some("fog"))
    assert("☁️" === dataPoint.emojiForIcon)
  }

  test("cloud has correct emoji ☁️") {
    val dataPoint = buildDataPoint(Some("cloudy"))
    assert("☁️" === dataPoint.emojiForIcon)
  }

  test("partly-cloudy-day has correct emoji ⛅") {
    val dataPoint = buildDataPoint(Some("partly-cloudy-day"))
    assert("⛅" === dataPoint.emojiForIcon)
  }

  test("partly-cloudy-night has correct emoji ☁︎") {
    val dataPoint = buildDataPoint(Some("partly-cloudy-night"))
    assert("☁︎" === dataPoint.emojiForIcon)
  }

  private def buildDataPoint(icon: Option[String]): DarkSky.DataPoint =
    new DarkSky.DataPoint(apparentTemperature = None, apparentTemperatureHigh = None, apparentTemperatureHighTime = None,
      apparentTemperatureLow = None, apparentTemperatureLowTime = None, apparentTemperatureMax = None,
      apparentTemperatureMaxTime = None, apparentTemperatureMin = None, apparentTemperatureMinTime = None,
      cloudCover = None, dewPoint = None, humidity = None, icon = icon, moonPhase = None, nearestStormBearing = None,
      nearestStormDistance = None, ozone = None, precipAccumulation = None, precipIntensity = None,
      precipIntensityMax = None, precipIntensityMaxTime = None, precipProbability = None, precipType = None,
      pressure = None, summary = None, sunriseTime = None, sunsetTime = None, temperature = None, temperatureHigh = None,
      temperatureHighTime = None, temperatureLow = None, temperatureLowTime = None, time = 1L, uvIndex = None,
      unIndexTime = None, visibility = None, windBearing = None, windGust = None, windSpeed = None)
}
