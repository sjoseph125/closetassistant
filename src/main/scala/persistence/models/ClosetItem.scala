package persistence.models

import zio.json.JsonEncoder
import zio.dynamodb.ProjectionExpression

final case class ClosetItem(
    closetItemKey: String,
    itemType: String
) derives JsonEncoder

object ClosetItem {
    implicit val schema: zio.schema.Schema.CaseClass2[String, String, ClosetItem] = zio.schema.DeriveSchema.gen[ClosetItem]
    val (closetItemKey, itemType) = ProjectionExpression.accessors[ClosetItem]
}

