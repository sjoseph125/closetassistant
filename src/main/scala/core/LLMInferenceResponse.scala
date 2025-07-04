package core

import zio.json.*
import zio.schema.Schema
import zio.schema.DeriveSchema

final case class LLMInferenceResponseRaw(
    response: String
) derives JsonDecoder

final case class LLMResponseAddItem(
    itemName: String,
    category: String,
    subCategory: String,
    colors: List[String] = List.empty,
    style: List[String] = List.empty,
    season: List[String] = List.empty, 
    warmth: String,
    pattern: String,
    fabric: String,
    activities: List[String] = List.empty,
    description: String,
    suggestedPairingColors: List[String] = List.empty
) derives JsonDecoder, JsonEncoder

final case class LLMResponseSearchOutfits(
    style: List[String] = List.empty,
    season: List[String] = List.empty,
    forcedItems: List[String] = List.empty,
    location: Option[String] = None
) derives JsonDecoder, JsonEncoder


final case class LLMInferenceResponse(
    responseAddItem: Option[LLMResponseAddItem] = None,
    responseSearchOutfits: Option[LLMResponseSearchOutfits] = None
) derives JsonEncoder