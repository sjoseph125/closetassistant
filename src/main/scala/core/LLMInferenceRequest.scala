package core
import zio.json.JsonEncoder

enum LLMRequestType:
  case AddNewItem
  case SearchOutfit

final case class LLMInferenceOptions(
    seed: Int
) derives JsonEncoder

final case class LLMResponsePropertyType(`type`: String) derives JsonEncoder

object LLMInferenceRequestAddItem {
  final case class LLMInferenceRequest(
      model: String,
      keep_alive: Int = -1,
      prompt: String,
      stream: Boolean = false,
      options: LLMInferenceOptions = LLMInferenceOptions(seed = 1654),
      format: LLMResponseFormatAddItem = LLMResponseFormatAddItem(),
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
        "description",
        "suggestedPairingColors"
      ),
      images: List[String] = List.empty
  ) derives JsonEncoder

  final case class LLMResponseFormatAddItem(
      `type`: String = "object",
      properties: LLMResponsePropertiesAddItem = LLMResponsePropertiesAddItem()
  ) derives JsonEncoder

  final case class LLMResponsePropertiesAddItem(
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
      suggestedPairingColors: LLMResponsePropertyType = LLMResponsePropertyType("array")
  ) derives JsonEncoder
}

object LLMInferenceRequestSearchOutfits {
  final case class LLMInferenceRequest(
      model: String,
      keep_alive: Int = -1,
      prompt: String,
      stream: Boolean = false,
      options: LLMInferenceOptions = LLMInferenceOptions(seed = 1654),
      format: LLMResponseFormatSearchOutfits = LLMResponseFormatSearchOutfits(),
      required: List[String] = List(
        "style",
        "season",
        "forcedItems",
        "location"
      )
  ) derives JsonEncoder
  final case class LLMResponseFormatSearchOutfits(
      `type`: String = "object",
      properties: LLMResponsePropertiesSearchOutfits =
        LLMResponsePropertiesSearchOutfits()
  ) derives JsonEncoder

  final case class LLMResponsePropertiesSearchOutfits(
      style: LLMResponsePropertyType = LLMResponsePropertyType("array"),
      season: LLMResponsePropertyType = LLMResponsePropertyType("array"),
      forcedItems: LLMResponsePropertyType = LLMResponsePropertyType("array"),
      location: LLMResponsePropertyType = LLMResponsePropertyType("array")
  ) derives JsonEncoder
}