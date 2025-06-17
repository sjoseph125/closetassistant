package core
import zio.json.JsonEncoder

final case class LLMInferenceRequest(
    model: String,
    prompt: String,
    stream: Boolean = false,
    options: LLMInferenceOptions = LLMInferenceOptions(seed = 12345),
    format: LLMResponseFormat,
    required: List[String] = List("color", "fabric", "activities", "season"), 
    images: List[String] = List.empty
) derives JsonEncoder

final case class LLMInferenceOptions(
    seed: Int
) derives JsonEncoder

final case class LLMResponseFormat(
    `type`: String = "object",
    properties: LLMResponseProperties
) derives JsonEncoder

final case class LLMResponseProperties(
    color: LLMResponsePropertyType = LLMResponsePropertyType("string"),
    fabric: LLMResponsePropertyType = LLMResponsePropertyType("string"),
    activities: LLMResponsePropertyType = LLMResponsePropertyType("array"),
    season: LLMResponsePropertyType = LLMResponsePropertyType("array")
) derives JsonEncoder

final case class LLMResponsePropertyType(`type`: String) derives JsonEncoder
