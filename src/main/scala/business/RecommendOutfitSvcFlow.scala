package business

import core.SearchRequest
import zio.*
import zio.http.*
import business.RecommendOutfitSvcFlow._
import core.SearchResponse
import core.LLMInferenceResponse
import web.layers.ServiceLayers.ExecutorAndPresignerType
import core.UserCloset
import persistence.models.UserClosetModel.userId

class RecommendOutfitSvcFlow(cfgCtx: CfgCtx) {

  import cfgCtx._

  def apply(
      request: SearchRequest
  ): ZIO[ExecutorAndPresignerType & Client, Throwable, SearchResponse] = {
    getUserCloset(request.userId).zipWithPar(
      llmSearchOutfits(request.searchCriteria)
    ) { (userClosetOpt, searchCriteria) =>
      val searchStyles =
        searchCriteria.responseSearchOutfits.map(_.style).toList.flatten
      val closetItems = userClosetOpt.toList.flatMap(_.closetItems)

      val filteredClosetItems = closetItems
        .withFilter(item =>
          item.itemMetadata.toList
            .flatMap(_.responseAddItem.toList.flatMap(_.style))
            .exists(searchStyles.contains)
        )
        .map(identity)
      print(s"Hey ${filteredClosetItems}")
      SearchResponse(
        userId = request.userId,
        results = filteredClosetItems.flatMap(_.itemName)
      )
    }
  }

}

object RecommendOutfitSvcFlow {
  case class CfgCtx(
      llmSearchOutfits: String => RIO[Client, LLMInferenceResponse],
      getUserCloset: String => URIO[ExecutorAndPresignerType, Option[
        UserCloset
      ]]
  )
}
