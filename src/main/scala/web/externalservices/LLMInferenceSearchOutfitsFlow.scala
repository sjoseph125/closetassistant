package web.externalservices

import zio.*
import zio.http.*
import zio.json.*
import core.*
import web.externalservices.LLMInferenceSearchOutfitsFlow.*

class LLMInferenceSearchOutfitsFlow(cfgCtx: CfgCtx) {
  import cfgCtx._

  def apply(
      request: String,
      userLocation: Option[Location] = None
  ): RIO[Client, LLMInferenceResponse] = {
    val updatedPrompt = userLocation match
      case None => prompt
        .replace("{USER_REQUEST}", request)
        .replace("{LATTITUDE}", "Not Provided")
        .replace("{LONGITUDE}", "Not Provided")
      case Some(value) =>
        prompt
          .replace("{USER_REQUEST}", request)
          .replace("{LATTITUDE}", value.latitude.toString)
          .replace("{LONGITUDE}", value.longitude.toString)
    
    for {
      request <- ZIO.fromEither(URL.decode(apiUrl)).map { url =>
        Request
          .post(
            url = url,
            body = Body.fromString(
              LLMInferenceRequestSearchOutfits
                .LLMInferenceRequest(
                  model = model,
                  prompt = updatedPrompt
                ).toJson
            )
          )
          .addHeader(Header.ContentType(MediaType.application.json))
      }
      res <- ZIO.serviceWithZIO[Client](client =>
        client.batched(request).flatMap(constructResult)
      )
    } yield res
  }

  private def constructResult(
      response: Response
  ): Task[LLMInferenceResponse] = {
    println(s"Recieved response ${response.body}")
    response.body.asString.flatMap(str =>
      ZIO
        .fromEither(str.fromJson[LLMInferenceResponseRaw])
        .fold(
          err =>
            println(s"Error at fromJson $str")
            throw new Exception(
              s"Failed to parse response with error: $err"
            )
          ,
          parsed =>
            LLMInferenceResponse(
              responseSearchOutfits = parsed.response
                .fromJson[LLMResponseSearchOutfits] match {
                case Right(llmResponse) => 
                  println(s"Parsed LLMResponseSearchOutfits: $llmResponse")
                  Some(llmResponse)
                case Left(error) =>
                  println(s"Failed with $error")
                  throw new Exception(
                    s"Failed to parse LLMResponse with error: $error"
                  )
              }
            )
        )
    )
  }

}

object LLMInferenceSearchOutfitsFlow {
  case class CfgCtx(
      apiUrl: String,
      model: String,
      prompt: String
  )
}
