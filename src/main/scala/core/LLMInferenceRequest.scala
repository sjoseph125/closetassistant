package core
import zio.json.JsonEncoder

final case class LLMInferenceRequest(
    model: String,
    prompt: String,
    stream: Boolean = false,
    options: LLMInferenceOptions = LLMInferenceOptions(seed = 1654),
    format: LLMResponseFormat = LLMResponseFormat(),
    required: List[String] = List(
      "itemName",
      "category",
      "subCategory",
      "colors",
      "style",
      "season",
      "warmth",
      "pattern",      
      "fabric",
      "activities",
      "description"
    ),
    images: List[String] = List.empty
) derives JsonEncoder

final case class LLMInferenceOptions(
    seed: Int
) derives JsonEncoder

final case class LLMResponseFormat(
    `type`: String = "object",
    properties: LLMResponseProperties = LLMResponseProperties()
) derives JsonEncoder

final case class LLMResponseProperties(
    itemName: LLMResponsePropertyType = LLMResponsePropertyType("string"),
    category: LLMResponsePropertyType = LLMResponsePropertyType("string"),
    subCategory: LLMResponsePropertyType = LLMResponsePropertyType("string"),
    colors: LLMResponsePropertyType = LLMResponsePropertyType("array"),
    style: LLMResponsePropertyType = LLMResponsePropertyType("array"),
    season: LLMResponsePropertyType = LLMResponsePropertyType("array"),
    warmth: LLMResponsePropertyType = LLMResponsePropertyType("string"),
    pattern: LLMResponsePropertyType = LLMResponsePropertyType("string"),
    fabric: LLMResponsePropertyType = LLMResponsePropertyType("string"),
    activities: LLMResponsePropertyType = LLMResponsePropertyType("array"),
    description: LLMResponsePropertyType = LLMResponsePropertyType("string"),
) derives JsonEncoder

final case class LLMResponsePropertyType(`type`: String) derives JsonEncoder
