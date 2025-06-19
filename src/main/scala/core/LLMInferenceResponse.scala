package core

import zio.json.*
import zio.schema.Schema
import zio.schema.DeriveSchema

final case class LLMInferenceResponseRaw(
    response: String
) derives JsonDecoder

final case class LLMResponse(
    color: String,
    fabric: String,
    activities: List[String],
    season: List[String]
) derives JsonDecoder, JsonEncoder


final case class LLMInferenceResponse(
    response: LLMResponse
) derives JsonEncoder