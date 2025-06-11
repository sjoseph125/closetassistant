package web.externalservices

import zio.*
import zio.http.*
import web.externalservices.LLMInferneceSvcFlow._
import core.SearchRequest
import scala.util.chaining.scalaUtilChainingOps

class LLMInferneceSvcFlow(cfgCtx: CfgCtx) {
  import cfgCtx._

  def postRequest(request: SearchRequest): RIO[Client, Response] = {
    // Implement the logic to send a request to the LLM inference service
    // using the provided configuration contextZClient[Any, Scope, Body, Throwable, Response]

    for {
      req <- ZIO.fromEither(URL.decode(apiUrl)).map { url =>
        Request
          .post(
            url = url,
            body = Body.fromString("""{"model": "gemma3:12b", "prompt": "what is the capital of France?", "stream": false}""")
          )
          .addHeader(Header.ContentType(MediaType.application.json))
      }
      res <- ZIO.serviceWithZIO[Client](client => client.batched(req))
    } yield res
      
  }
}

object LLMInferneceSvcFlow {

  case class CfgCtx(
      apiUrl: String
  )
}
