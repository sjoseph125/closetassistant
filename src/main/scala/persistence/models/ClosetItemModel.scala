package persistence.models

import zio.json.JsonEncoder
import zio.dynamodb.ProjectionExpression
import zio.json.JsonDecoder
import zio.schema.{Schema, DeriveSchema}
import core.LLMInferenceResponse

final case class ClosetItemModel(
    closetItemKey: String,
    itemType: String,
    presignedUrl: Option[String] = None,
    itemMetadata: LLMInferenceResponse
) derives JsonEncoder

object ClosetItemModel {
    implicit val schema: Schema.CaseClass4[String, String, Option[String], LLMInferenceResponse, ClosetItemModel] = zio.schema.DeriveSchema.gen[ClosetItemModel]
    val (closetItemKey, itemType, presignedUrl, itemMetadata) = ProjectionExpression.accessors[ClosetItemModel]
}

