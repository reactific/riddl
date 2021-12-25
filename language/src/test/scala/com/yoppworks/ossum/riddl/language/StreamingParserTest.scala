package com.yoppworks.ossum.riddl.language

import com.yoppworks.ossum.riddl.language.AST.*

/** Unit Tests For StreamingParser */
class StreamingParserTest extends ParsingTest {

  "StreamingParser" should {
    "recognize a Processor" in {
      val input = """
                    |processor GetWeatherForecast is {
                    |  outlet Weather is Forecast
                    |} described by "This is a source for Forecast data"
                    |""".stripMargin
      val expected = Processor(
        1 -> 1,
        Identifier(2 -> 11, "GetWeatherForecast"),
        List.empty[Inlet],
        List(Outlet(
          3 -> 3,
          Identifier(3 -> 10, "Weather"),
          TypeRef(3 -> 21, PathIdentifier(3 -> 21, List("Forecast")))
        )),
        Some(Description(
          4 -> 3,
          List(LiteralString(4 -> 16, "This is a source for Forecast data")),
          List(),
          None,
          Map(),
          List()
        ))
      )
      checkDefinition[Processor, Processor](input, expected, identity)
    }
    "recognize a Pipe" in {
      val input = """
                    |pipe TemperatureChanges is { transmit temperature }
                    |""".stripMargin
      val expected = Pipe(
        1 -> 1,
        Identifier(2 -> 6, "TemperatureChanges"),
        TypeRef(2 -> 39, PathIdentifier(2 -> 39, List("temperature")))
      )
      checkDefinition[Pipe, Pipe](input, expected, identity)
    }

    "recognize an InJoint" in {
      val input =
        """joint temp_in is inlet GetCurrentTemperature.weather from pipe WeatherForecast"""
      val expected = Joint(
        1 -> 1,
        Identifier(1 -> 7, "temp_in"),
        DefRef[Inlet](1 -> 18, PathIdentifier(1 -> 24, Seq("weather", "GetCurrentTemperature"))),
        DefRef[Pipe](1 -> 59, PathIdentifier(1 -> 64, Seq("WeatherForecast")))
      )
      checkDefinition[Joint, Joint](input, expected, identity)
    }
    "recognize an OutJoint" in {
      val input = """joint forecast is outlet GetWeatherForecast.Weather to pipe WeatherForecast"""
      val expected = Joint(
        1 -> 1,
        Identifier(1 -> 7, "forecast"),
        DefRef[Outlet](1 -> 19, PathIdentifier(1 -> 26, Seq("Weather", "GetWeatherForecast"))),
        DefRef[Pipe](1 -> 56, PathIdentifier(1 -> 61, Seq("WeatherForecast")))
      )
      checkDefinition[Joint, Joint](input, expected, identity)
    }
    "recognize a Plant" in {
      val input =
        """
          |domain AnyDomain is {
          |plant SensorMaintenance is {
          |
          |  processor GetWeatherForecast is {
          |    outlet Weather is Forecast
          |  } described by "This is a source for Forecast data"
          |
          |  processor GetCurrentTemperature is {
          |    inlet Weather is Forecast
          |    outlet CurrentTemp is Temperature
          |  } explained as "This is a Flow for the current temperature, when it changes"
          |
          |  processor AttenuateSensor is {
          |    inlet CurrentTemp is Temperature
          |  } explained as "This is a Sink for making sensor adjustments based on temperature"
          |
          |  pipe WeatherForecast is {
          |    transmit Forecast
          |  } explained as "Carries changes in the current weather forecast"
          |
          |  pipe TemperatureChanges is {
          |    transmit temperature
          |  } explained as "Carries changes in the current temperature"
          |
          |  joint forecast is outlet GetWeatherForecast.Weather to pipe WeatherForecast
          |  joint temp_in is inlet GetCurrentTemperature.weather from pipe WeatherForecast
          |  joint temp_out is outlet GetCurrentTemperature.CurrentTemp to pipe TemperatureChanges
          |  joint temp_changes is inlet AttenuateSensor.CurrentTemp from pipe TemperatureChanges
          |
          |} explained as
          |"A complete plant definition for temperature based sensor attenuation."
          |
          |} explained as "Plants can only be specified in a domain definition"
          |""".stripMargin
      val expected = Plant(
        3 -> 1,
        Identifier(3 -> 7, "SensorMaintenance"),
        List(
          Pipe(
            18 -> 3,
            Identifier(18 -> 8, "WeatherForecast"),
            TypeRef(19 -> 14, PathIdentifier(19 -> 14, List("Forecast"))),
            Some(Description(
              20 -> 5,
              List(LiteralString(20 -> 18, "Carries changes in the current weather forecast")),
              List(),
              None,
              Map(),
              List()
            ))
          ),
          Pipe(
            22 -> 3,
            Identifier(22 -> 8, "TemperatureChanges"),
            TypeRef(23 -> 14, PathIdentifier(23 -> 14, List("temperature"))),
            Some(Description(
              24 -> 5,
              List(LiteralString(24 -> 18, "Carries changes in the current temperature")),
              List(),
              None,
              Map(),
              List()
            ))
          )
        ),
        List(
          Processor(
            5 -> 3,
            Identifier(5 -> 13, "GetWeatherForecast"),
            List(),
            List(Outlet(
              6 -> 5,
              Identifier(6 -> 12, "Weather"),
              TypeRef(6 -> 23, PathIdentifier(6 -> 23, List("Forecast"))),
              None
            )),
            Some(Description(
              7 -> 5,
              List(LiteralString(7 -> 18, "This is a source for Forecast data")),
              List(),
              None,
              Map(),
              List()
            ))
          ),
          Processor(
            9 -> 3,
            Identifier(9 -> 13, "GetCurrentTemperature"),
            List(Inlet(
              10 -> 5,
              Identifier(10 -> 11, "Weather"),
              TypeRef(10 -> 22, PathIdentifier(10 -> 22, List("Forecast"))),
              None
            )),
            List(Outlet(
              11 -> 5,
              Identifier(11 -> 12, "CurrentTemp"),
              TypeRef(11 -> 27, PathIdentifier(11 -> 27, List("Temperature"))),
              None
            )),
            Some(Description(
              12 -> 5,
              List(LiteralString(
                12 -> 18,
                "This is a Flow for the current temperature, when it changes"
              )),
              List(),
              None,
              Map(),
              List()
            ))
          ),
          Processor(
            14 -> 3,
            Identifier(14 -> 13, "AttenuateSensor"),
            List(Inlet(
              15 -> 5,
              Identifier(15 -> 11, "CurrentTemp"),
              TypeRef(15 -> 26, PathIdentifier(15 -> 26, List("Temperature"))),
              None
            )),
            List(),
            Some(Description(
              16 -> 5,
              List(LiteralString(
                16 -> 18,
                "This is a Sink for making sensor adjustments based on temperature"
              )),
              List(),
              None,
              Map(),
              List()
            ))
          )
        ),
        List(
          Joint(
            26 -> 3,
            Identifier(26 -> 9, "forecast"),
            DefRef(26 -> 21, PathIdentifier(26 -> 28, List("Weather", "GetWeatherForecast"))),
            DefRef(26 -> 58, PathIdentifier(26 -> 63, List("WeatherForecast"))),
            None
          ),
          Joint(
            27 -> 3,
            Identifier(27 -> 9, "temp_in"),
            DefRef(27 -> 20, PathIdentifier(27 -> 26, List("weather", "GetCurrentTemperature"))),
            DefRef(27 -> 61, PathIdentifier(27 -> 66, List("WeatherForecast"))),
            None
          ),
          Joint(
            28 -> 3,
            Identifier(28 -> 9, "temp_out"),
            DefRef(
              28 -> 21,
              PathIdentifier(28 -> 28, List("CurrentTemp", "GetCurrentTemperature"))
            ),
            DefRef(28 -> 65, PathIdentifier(28 -> 70, List("TemperatureChanges"))),
            None
          ),
          Joint(
            29 -> 3,
            Identifier(29 -> 9, "temp_changes"),
            DefRef(29 -> 25, PathIdentifier(29 -> 31, List("CurrentTemp", "AttenuateSensor"))),
            DefRef(29 -> 64, PathIdentifier(29 -> 69, List("TemperatureChanges"))),
            None
          )
        ),
        Some(Description(
          31 -> 3,
          List(LiteralString(
            32 -> 1,
            "A complete plant definition for temperature based sensor attenuation."
          ))
        ))
      )
      checkDefinition[Domain, Plant](input, expected, _.plants.head)
    }
  }
}

// joint forecast is outlet GetWeatherForecast.Weather to WeatherForecast