package web.externalservices

import zio.*
import zio.http.*
import zio.aws.s3.S3
import java.util.Base64

import zio.json.*
import web.externalservices.LLMInferneceAddItemsFlow._
import web.layers.ServiceLayers.ClientAndS3

import core.*

class LLMInferneceAddItemsFlow(cfgCtx: CfgCtx) {
  import cfgCtx._

  def postRequest(
      request: PerformInference
  ): RIO[ClientAndS3, Map[String, LLMInferenceResponse]] = {
    // Implement the logic to send a request to the LLM inference service
    // using the provided configuration contextZClient[Any, Scope, Body, Throwable, Response]

    ZIO
      .foreachPar(request.closetItemKeys) { imageId =>
        for {
          enacodedImage <- s3Download(s"${request.imageRepoId}/$imageId").map(
            chunk => Base64.getEncoder.encodeToString(chunk.toArray)
          )
          request <- ZIO.fromEither(URL.decode(apiUrl)).map { url =>
            Request
              .post(
                url = url,
                body = constructBody(enacodedImage)
              )
              .addHeader(Header.ContentType(MediaType.application.json))
          }
          res <- ZIO.serviceWithZIO[Client](client =>
            client.batched(request).flatMap(constructResult(imageId, _))
          )
        } yield res
      }
      .map(_.foldLeft(Map.empty[String, LLMInferenceResponse])(_ ++ _))
  }

  private def constructBody(enacodedImage: String): Body =
    // Construct the body for the request
    Body.fromString(
      LLMInferenceRequestAddItem
        .LLMInferenceRequest(
          model = model,
          prompt = prompt,
          images = List(enacodedImage)
        )
        .toJson
    )

  private def constructResult(
      imageId: String,
      response: Response
  ): Task[Map[String, LLMInferenceResponse]] = {
    println(s"Recieved response ${response.body}")
    response.body.asString.flatMap(str =>
      ZIO
        .fromEither(str.fromJson[LLMInferenceResponseRaw])
        .fold(
          err =>
            println(s"Error at fromJson $str")
            throw new Exception(
              s"Failed to parse response for imageId $imageId: $err"
            )
          ,
          parsed =>
            println(parsed.response)
            Map(
              imageId -> LLMInferenceResponse(
                responseAddItem = parsed.response.fromJson[LLMResponseAddItem] match {
                  case Right(llmResponse) => Some(llmResponse)
                  case Left(error) =>
                    println(s"Failed with $error")
                    throw new Exception(
                      s"Failed to parse LLMResponse for imageId $imageId: $error"
                    )
                }
              )
            )
        )
    )
  }
}

object LLMInferneceAddItemsFlow {

  case class CfgCtx(
      apiUrl: String,
      s3Download: String => URIO[S3, Chunk[Byte]],
      model: String,
      prompt: String
  )
}
