package core

import zio.json.{JsonEncoder, JsonDecoder, DeriveJsonDecoder}
import core.OutfitTemplates.{BasicTemplate, DressTemplate}

final case class SearchRequest(
    userId: String,
    searchCriteria: String
) derives JsonEncoder

final case class SearchResponse(
    basicOutfits: List[BasicTemplate] = List.empty,
    dressOutfits: List[DressTemplate] = List.empty
) derives JsonEncoder

object SearchRequest {

  implicit val decoder: JsonDecoder[SearchRequest] = DeriveJsonDecoder.gen[SearchRequest]
}
