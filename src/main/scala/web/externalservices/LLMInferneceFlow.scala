package web.externalservices

import zio.*
import zio.http.*
import web.externalservices.LLMInferneceFlow._
import core.{
  PerformInference,
  LLMInferenceRequest,
  LLMResponseFormat,
  LLMResponseProperties,
  LLMResponsePropertyType
}
import web.layers.ServiceLayers.ClientAndS3
import scala.util.chaining.scalaUtilChainingOps
import zio.aws.s3.S3
import java.util.Base64 // Import the Base64 utility
import zio.json.* // Import zio-json syntax for .toJson extension method

// Ensure implicit JsonCodec for LLMInferenceRequest is in scope
import core.LLMInferenceRequest // Ensure the type is imported

class LLMInferneceFlow(cfgCtx: CfgCtx) {
  import cfgCtx._

  def postRequest(
      request: PerformInference
  ): RIO[ClientAndS3, List[Response]] = {
    // Implement the logic to send a request to the LLM inference service
    // using the provided configuration contextZClient[Any, Scope, Body, Throwable, Response]

    ZIO.foreachPar(request.closetItemKeys) { imageId =>
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
        res <- ZIO.serviceWithZIO[Client](client => client.batched(request))
        _ = println(res.body.asString)
      } yield res

    }
  }

  private def constructBody(enacodedImage: String): Body =
    // Construct the body for the request
    Body.fromString(
      LLMInferenceRequest(
        model = model,
        prompt = prompt,
        format = LLMResponseFormat(
          properties = LLMResponseProperties()
        ),
        images = List(enacodedImage)
      ).toJson
    )
}

object LLMInferneceFlow {

  case class CfgCtx(
      apiUrl: String,
      s3Download: String => URIO[S3, Chunk[Byte]],
      model: String,
      prompt: String
  )
}
