package persistence.models

import zio.json.JsonEncoder
import zio.dynamodb.ProjectionExpression
import zio.json.JsonDecoder
import zio.schema.{Schema, DeriveSchema}
import core.LLMInferenceResponse
import core.LLMResponseAddItem

final case class ClosetItemModel(
    closetItemKey: String,
    itemType: Option[String] = None,
    presignedUrl: Option[String] = None,
    itemMetadata: Option[LLMResponseAddItem] = None,
    itemName: Option[String] = None
) derives JsonEncoder

object ClosetItemModel {
    implicit val schema: Schema.CaseClass5[String, Option[String], Option[String], Option[LLMResponseAddItem], Option[String], ClosetItemModel] = zio.schema.DeriveSchema.gen[ClosetItemModel]
    val (closetItemKey, itemType, presignedUrl, itemMetadata, itemName) = ProjectionExpression.accessors[ClosetItemModel]
}

