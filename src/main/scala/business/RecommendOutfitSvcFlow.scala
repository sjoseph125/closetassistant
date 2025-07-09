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
import persistence.models.ClosetItemModel
import scala.collection.View.Filter
import core.OutfitTemplates.BasicTemplate
import core.OutfitTemplates.DressTemplate

class RecommendOutfitSvcFlow(cfgCtx: CfgCtx) {

  import cfgCtx._

  def apply(
      request: SearchRequest
  ): RIO[ExecutorAndPresignerType & Client, SearchResponse] = {
    println(
      s"Starting RecommendOutfitSvcFlow for user ${request.userId} with search criteria: ${request.searchCriteria}"
    )
    for {
      sortedClosetItmes <- sortItemsByCategory(request)
      createdOutfits <- createOutfits(sortedClosetItmes)
    } yield createdOutfits
  }

  private def sortItemsByCategory(
      request: SearchRequest
  ): RIO[ExecutorAndPresignerType & Client, Map[FilterType, List[
    ClosetItemModel
  ]]] = {
    for {
      (userClosetOpt, searchCriteria) <- getUserCloset(request.userId, true)
        .zipWithPar(llmSearchOutfits(request.searchCriteria)) {
          (userClosetOpt, searchCriteria) => (userClosetOpt, searchCriteria)
        }
      _ = println(s"Search criteria: $searchCriteria")
      searchStyles = searchCriteria.responseSearchOutfits
        .map(_.style)
        .toList
        .flatten
      _ = println(s"Search styles: $searchStyles")
      filteredClosetItems = userClosetOpt.toList
        .flatMap(_.closetItems)
        .filter(item =>
          item.itemMetadata.toList.flatMap(_.style)
            .exists(searchStyles.contains)
        )

      resultMap <- ZIO
        .collectAllPar(
          CATEGORIES.map(category =>
            ZIO.attempt(filterItemsByCategory(filteredClosetItems, category))
          )
        )
        .map(_.toMap)
        .withParallelism(5)
    } yield resultMap
  }

  private def createOutfits(
      sortedClosetItmes: Map[FilterType, List[ClosetItemModel]]
  ): Task[SearchResponse] = {
    createBasicOutfits(
      sortedClosetItmes(FilterType.TOP),
      sortedClosetItmes(FilterType.BOTTOM),
      sortedClosetItmes(FilterType.SHOES),
      sortedClosetItmes(FilterType.OUTERWEAR)
    ).zipWithPar(
      createDressOutfits(
        sortedClosetItmes(FilterType.DRESS),
        sortedClosetItmes(FilterType.SHOES)
      )
    ) { (basicOutfits, dressOutfits) =>
      SearchResponse(
        basicOutfits = basicOutfits,
        dressOutfits = dressOutfits
      )
    }
  }

  private def filterItemsByCategory(
      filteredClosetItems: List[ClosetItemModel],
      filterType: FilterType
  ): (FilterType, List[ClosetItemModel]) = {

    filterType ->
      filteredClosetItems
        .withFilter(item =>
          item.itemMetadata
            .map(_.category)
            .contains(filterType.value)
        )
        .map(identity)
  }

  private def createBasicOutfits(
      tops: List[ClosetItemModel],
      bottoms: List[ClosetItemModel],
      shoes: List[ClosetItemModel],
      outerwear: List[ClosetItemModel]
  ): Task[List[BasicTemplate]] = {
    println(
      s"Creating basic outfits with tops: ${tops.map(_.closetItemKey)}, bottoms: ${bottoms.map(_.closetItemKey)}, shoes: ${shoes.map(_.closetItemKey)}"
    )
    ZIO.attempt{
      val outfits = tops.flatMap(top =>
        bottoms.flatMap(bottom =>
          shoes.map(shoe =>
            BasicTemplate(
              top = top,
              bottom = bottom,
              shoes = shoe
            )
          )
        )
      )
      if (outerwear.isEmpty) {
        outfits
      } else {
        outerwear.flatMap(outer =>
          outfits.map(outfit =>
            outfit.copy(outerwear = Some(outer))
          )
        )
      }
  }
  }

  private def createDressOutfits(
      dresses: List[ClosetItemModel],
      shoes: List[ClosetItemModel]
  ): Task[List[DressTemplate]] = {
    ZIO.attempt(
      dresses.flatMap(dress =>
        shoes.map(shoe =>
          DressTemplate(
            dress = dress,
            shoes = shoe
          )
        )
      )
    )
  }

}

object RecommendOutfitSvcFlow {
  enum FilterType(val value: String) {
    case TOP extends FilterType("Top")
    case BOTTOM extends FilterType("Bottom")
    case DRESS extends FilterType("Dress")
    case SHOES extends FilterType("Shoes")
    case OUTERWEAR extends FilterType("Outerwear")
  }
  val CATEGORIES = FilterType.values.toSet

  case class CfgCtx(
      llmSearchOutfits: String => RIO[Client, LLMInferenceResponse],
      getUserCloset: (String, Boolean) => URIO[ExecutorAndPresignerType, Option[
        UserCloset
      ]]
  )
}
