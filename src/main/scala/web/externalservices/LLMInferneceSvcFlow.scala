package web.externalservices

import zio.*
import zio.http.*
import web.externalservices.LLMInferneceSvcFlow._
import core.PerformInference
import scala.util.chaining.scalaUtilChainingOps
import zio.aws.s3.S3
import java.util.Base64 // Import the Base64 utility

class LLMInferneceSvcFlow(cfgCtx: CfgCtx) {
  import cfgCtx._

  def postRequest(request: PerformInference): RIO[Client & S3, Response] = {
    // Implement the logic to send a request to the LLM inference service
    // using the provided configuration contextZClient[Any, Scope, Body, Throwable, Response]

    val a: ZIO[S3, Nothing, Map[String, String]] = ZIO.foreachPar(request.closetItemKeys) { imageId =>
        s3Download(s"${request.imageRepoId}/$imageId").map(
          chunk => 
            imageId -> Base64.getEncoder.encodeToString(chunk.toArray)
        )    
      }.map(_.toMap)

    request.closetItemKeys.headOption match {
      case Some(imageId) =>
        for {
          chunk <- s3Download(s"${request.imageRepoId}/$imageId")
          req <- ZIO.fromEither(URL.decode(apiUrl)).map { url =>
            Request
              .post(
                url = url,
                body = Body.fromString(
                  """{"model": "gemma3:12b", "prompt": "what is the capital of France?", "stream": false}"""
                )
              )
              .addHeader(Header.ContentType(MediaType.application.json))
          }
          res <- ZIO.serviceWithZIO[Client](client => client.batched(req))
        } yield res
      case None =>
        ZIO.fail(new IllegalArgumentException("No image keys provided"))
    }
  }
}

object LLMInferneceSvcFlow {

  case class CfgCtx(
      apiUrl: String,
      s3Download: String => URIO[S3, Chunk[Byte]]
  )
}
