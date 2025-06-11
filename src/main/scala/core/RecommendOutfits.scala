package core

import zio.json.{JsonEncoder, JsonDecoder, DeriveJsonDecoder}

final case class SearchRequest(
    userId: String,
    searchCriteria: String
) derives JsonEncoder

final case class SearchResponse(
    userId: String,
    results: List[String]
) derives JsonEncoder

object SearchRequest {

  implicit val decoder: JsonDecoder[SearchRequest] = DeriveJsonDecoder.gen[SearchRequest]
}
