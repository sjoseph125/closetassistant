package web.externalservices

import zio.*
import zio.http.*
import zio.json.*
import core.*
import web.externalservices.WeatherInfoFlow.CfgCtx

class WeatherInfoFlow(cfgCtx: CfgCtx) {
  import cfgCtx._

  def apply(
      userLocation: Location
  ): RIO[Client, WeatherInfoResponse] = {
    for {
      request <- ZIO.fromEither(URL.decode(apiUrl)).map { url =>
        Request
          .get(url = url.copy(
            queryParams = QueryParams(
              "key" -> apiKey,
              "q" -> s"${userLocation.latitude},${userLocation.longitude}"
            )
          ))
          .addHeader(Header.ContentType(MediaType.application.json))
      }
      res <- ZIO.serviceWithZIO[Client](client =>
        client.batched(request).flatMap(constructResult)
      )
    } yield res
  }

  private def constructResult(
      response: Response
  ) = {
    println(s"Received response ${response.body}")
    println(s"Response: ${response.body.asString}")
    response.body.asString.fold(
      err => throw new Exception(s"Failed to get weather info: $err"),
      str => str.fromJson[WeatherInfoResponse] match {
        case Left(jsonErr) =>
          throw new Exception(s"Failed to parse response with error: $jsonErr")
        case Right(parsed) =>
          parsed
      }
    )
  }
}

object WeatherInfoFlow {
    case class CfgCtx(
        apiUrl: String,
        apiKey: String)
}