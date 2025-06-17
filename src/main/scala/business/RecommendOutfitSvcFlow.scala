package business

import core.SearchRequest
import zio.*
import zio.http.*
import business.RecommendOutfitSvcFlow._
import core.SearchResponse

class RecommendOutfitSvcFlow(cfgCtx: CfgCtx) {

  import cfgCtx._

  def apply(request: SearchRequest): ZIO[Client, Throwable, SearchResponse] = {
    // Implement the logic to send a request to the recommendation service
    // using the provided configuration context

    // inferSearchRequest(request)
    //   .tapError(err => ZIO.logError(s"Failed to send search request: $err"))
    //   .flatMap(response => {
        // println(response.body.asString)
        ZIO.succeed(
          SearchResponse(
            userId = request.userId,
            results =
              List("outfit1", "outfit2", "outfit3") // Placeholder results
          )
        )
    //   })
  }

}

object RecommendOutfitSvcFlow {
  case class CfgCtx(
    //   inferSearchRequest: SearchRequest => RIO[Client, Response]
  )
}
