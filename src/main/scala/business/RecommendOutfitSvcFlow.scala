package business
import zio.*
import zio.http.*
import business.RecommendOutfitSvcFlow._
import core.*
import web.layers.ServiceLayers.ExecutorAndPresignerType
import persistence.models.ClosetItemModel
import core.OutfitTemplates.{BasicTemplate, DressTemplate, LayeredTemplate}
import scala.collection.immutable.Range.Inclusive

class RecommendOutfitSvcFlow(cfgCtx: CfgCtx) {

  import cfgCtx._

  def apply(
      request: SearchRequest
  ): RIO[ExecutorAndPresignerType & Client, SearchResponse] = {
    println(
      s"Starting RecommendOutfitSvcFlow for user ${request.userId} with search criteria: ${request.searchCriteria}"
    )
    for {
      searchCriteria <- llmSearchOutfits(
        request.searchCriteria,
        request.userLocation
      )
      sortedClosetItemsFiber <- sortItemsByCategory(
        request.userId,
        searchCriteria
      ).fork
      userLocationFromSearch = searchCriteria.responseSearchOutfits.map(
        _.location
      )
      latitudeOpt = userLocationFromSearch.flatMap(_.headOption)
      longitudeOpt = userLocationFromSearch.flatMap(_.tail.headOption)
      weatherInfoOpt <- (
        userLocationFromSearch,
        request.userLocation,
        latitudeOpt,
        longitudeOpt
      ) match {
        case (Some(location), _, Some(latitude), Some(longitude)) =>
          weatherInfoFlow(Location(latitude.toDouble, longitude.toDouble))
            .map(Some(_))
        case (None, Some(currentUserLocation), _, _) =>
          weatherInfoFlow(currentUserLocation).map(Some(_))
        case _ => ZIO.succeed(None)
      }

      createdOutfits <- sortedClosetItemsFiber.join.flatMap(sortedClosetItems =>
        createOutfits(sortedClosetItems, weatherInfoOpt)
      )
    } yield createdOutfits
  }

  private def sortItemsByCategory(
      userId: String,
      searchCriteria: LLMInferenceResponse
  ): RIO[ExecutorAndPresignerType & Client, Map[FilterType, List[
    ClosetItemModel
  ]]] = {
    for {
      userClosetOpt <- getUserCloset(userId, true)
      searchStyles = searchCriteria.responseSearchOutfits
        .map(_.style)
        .toList
        .flatten
      filteredClosetItems = userClosetOpt.toList
        .flatMap(_.closetItems)
        .filter(item =>
          item.itemMetadata.toList
            .flatMap(_.style)
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
      sortedClosetItmes: Map[FilterType, List[ClosetItemModel]],
      weatherInfoOpt: Option[WeatherInfoResponse]
  ): Task[SearchResponse] = {
    for {
      basicFiber <- createBasicOutfits(
        sortedClosetItmes(FilterType.TOP),
        sortedClosetItmes(FilterType.BOTTOM),
        sortedClosetItmes(FilterType.SHOES),
        sortedClosetItmes(FilterType.OUTERWEAR)
      ).fork
      dressFiber <- createDressOutfits(
        sortedClosetItmes(FilterType.DRESS),
        sortedClosetItmes(FilterType.SHOES)
      ).fork
      layeredFiber <- createLayeredOutfits(
        sortedClosetItmes(FilterType.TOP),
        sortedClosetItmes(FilterType.BOTTOM),
        sortedClosetItmes(FilterType.SHOES),
        sortedClosetItmes(FilterType.OUTERWEAR)
      ).fork
      basicOutfits <- basicFiber.join
      dressOutfits <- dressFiber.join
      layeredOutfits <- layeredFiber.join
      filteredByWeather <- filterByWeather(
        basicOutfits,
        dressOutfits,
        layeredOutfits,
        weatherInfoOpt
      )

    } yield filteredByWeather
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

    ZIO.attempt {
      tops.flatMap(top =>
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

  // Add a stub implementation for createLayeredOutfits
  private def createLayeredOutfits(
      tops: List[ClosetItemModel],
      bottoms: List[ClosetItemModel],
      shoes: List[ClosetItemModel],
      outerwear: List[ClosetItemModel]
  ): Task[List[LayeredTemplate]] = {
    ZIO.attempt {
      outerwear.flatMap(outer =>
        tops.flatMap(top =>
          bottoms.flatMap(bottom =>
            shoes.map(shoe =>
              LayeredTemplate(
                top = top,
                bottom = bottom,
                shoes = shoe,
                outerwear = outer
              )
            )
          )
        )
      )
    }
  }

  private def filterByWeather(
      basicOutfits: List[BasicTemplate],
      dressOutfits: List[DressTemplate],
      layeredOutfits: List[LayeredTemplate],
      weatherInfoOpt: Option[WeatherInfoResponse]
  ): ZIO[Any, Throwable, SearchResponse] = {
    weatherInfoOpt match {
      case Some(weatherInfo) => {
        val temp: Inclusive = getCurrentTemperature(weatherInfo)
        println(s"Current temperature range: $temp")
        val filteredBasicFiber = ZIO.attempt {
          basicOutfits.withFilter(bt => temp.contains(calculateTotalWarmth(bt))).map(bt =>
            bt.copy(
              top = bt.top.copy(itemMetadata = None),
              bottom = bt.bottom.copy(itemMetadata = None),
              shoes = bt.shoes.copy(itemMetadata = None)
            )
          )
        }.fork

        val filteredDressFiber = ZIO.attempt {
          dressOutfits.withFilter(dt => temp.contains(calculateTotalWarmth(dt))).map(dt =>
            dt.copy(
              dress = dt.dress.copy(itemMetadata = None),
              shoes = dt.shoes.copy(itemMetadata = None)
            )
          )
        }.fork

        val filteredLayeredFiber = ZIO.attempt {
          layeredOutfits.withFilter(lt => temp.contains(calculateTotalWarmth(lt))).map(lt =>
            lt.copy(
              top = lt.top.copy(itemMetadata = None),
              bottom = lt.bottom.copy(itemMetadata = None),
              shoes = lt.shoes.copy(itemMetadata = None),
              outerwear = lt.outerwear.copy(itemMetadata = None)
            )
          )
        }.fork

        for {
          filteredBasic <- filteredBasicFiber.flatMap(_.join)
          filteredDress <- filteredDressFiber.flatMap(_.join)
          filteredLayered <- filteredLayeredFiber.flatMap(_.join)
        } yield SearchResponse(filteredBasic, filteredDress, filteredLayered)
      }
      case None => ZIO.succeed(SearchResponse(basicOutfits, dressOutfits, layeredOutfits))
    }
  }

  private def getCurrentTemperature(
      weatherInfoOpt: WeatherInfoResponse
  ): Inclusive = {
    math.round(weatherInfoOpt.current.temp_f) match {
        case temp if 80 <= temp              => 5 to 15
        case temp if 70 <= temp && temp < 80 => 10 to 15
        case temp if 60 <= temp && temp < 70 => 15 to 20
        case _                               => 25 to 30
    }
  }

  private def calculateTotalWarmth(
      outfit: BasicTemplate | DressTemplate | LayeredTemplate
  ): Int = {
    outfit match {
      case basic: BasicTemplate =>
        findItemWarmth(basic.top) +
          findItemWarmth(basic.bottom) +
          findItemWarmth(basic.shoes)
      case dress: DressTemplate =>
        findItemWarmth(dress.dress) +
          findItemWarmth(dress.shoes)
      case layered: LayeredTemplate =>
        findItemWarmth(layered.top) +
          findItemWarmth(layered.bottom) +
          findItemWarmth(layered.shoes) +
          findItemWarmth(layered.outerwear)
    }
  }

  private def findItemWarmth(
      item: ClosetItemModel
  ): Int = item.itemMetadata.map(_.warmth.replace(",", "").trim.toInt).getOrElse(0)

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
      llmSearchOutfits: (
          String,
          Option[Location]
      ) => RIO[Client, LLMInferenceResponse],
      getUserCloset: (String, Boolean) => URIO[ExecutorAndPresignerType, Option[
        UserCloset
      ]],
      weatherInfoFlow: Location => RIO[Client, WeatherInfoResponse]
  )
}
