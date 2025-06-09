package persistence.models

import zio.json.JsonEncoder
import zio.dynamodb.ProjectionExpression
import zio.json.JsonDecoder
import zio.schema.{Schema, DeriveSchema}

final case class ClosetItemModel(
    closetItemKey: String,
    itemType: String
) derives JsonEncoder

object ClosetItemModel {
    implicit val schema: Schema.CaseClass2[String, String, ClosetItemModel] = zio.schema.DeriveSchema.gen[ClosetItemModel]
    val (closetItemKey, itemType) = ProjectionExpression.accessors[ClosetItemModel]
}

