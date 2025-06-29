package core

import zio.json.*
import zio.schema.Schema
import zio.schema.DeriveSchema

final case class LLMInferenceResponseRaw(
    response: String
) derives JsonDecoder

final case class LLMResponse(
    itemName: String,
    category: String,
    subCategory: String,
    colors: List[String] = List.empty,
    style: List[String] = List.empty,
    fabric: String,
    activities: List[String] = List.empty,
    season: List[String] = List.empty,
    warmth: String,
    description: String,
    pattern: String
) derives JsonDecoder, JsonEncoder


final case class LLMInferenceResponse(
    response: LLMResponse
) derives JsonEncoder