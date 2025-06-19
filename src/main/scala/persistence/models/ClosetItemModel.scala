package persistence.models

import zio.json.JsonEncoder
import zio.dynamodb.ProjectionExpression
import zio.json.JsonDecoder
import zio.schema.{Schema, DeriveSchema}
import core.LLMInferenceResponse

final case class ClosetItemModel(
    closetItemKey: String,
    itemType: String,
    itemMetadata: LLMInferenceResponse
) derives JsonEncoder

object ClosetItemModel {
    implicit val schema: Schema.CaseClass3[String, String, LLMInferenceResponse, ClosetItemModel] = zio.schema.DeriveSchema.gen[ClosetItemModel]
    val (closetItemKey, itemType, itemMetadata) = ProjectionExpression.accessors[ClosetItemModel]
}

