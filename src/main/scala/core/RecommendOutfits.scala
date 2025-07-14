package core

import zio.json.{JsonEncoder, JsonDecoder, DeriveJsonDecoder}
import core.OutfitTemplates.{BasicTemplate, DressTemplate}
import core.OutfitTemplates.LayeredTemplate

final case class SearchRequest(
    userId: String,
    searchCriteria: String,
    userLocation: Option[Location] = None
) derives JsonDecoder

final case class Location(
    latitude: Double,
    longitude: Double
) derives JsonDecoder

final case class SearchResponse(
    basicOutfits: List[BasicTemplate] = List.empty,
    dressOutfits: List[DressTemplate] = List.empty,
    layeredOutfits: List[LayeredTemplate] = List.empty,
) derives JsonEncoder

// object SearchRequest {

//   implicit val decoder: JsonDecoder[SearchRequest] = DeriveJsonDecoder.gen[SearchRequest]
// }
