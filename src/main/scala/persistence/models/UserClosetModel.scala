package persistence.models

import zio.schema.*
import zio.dynamodb.ProjectionExpression

final case class UserClosetModel(userId: String, closetItemKeys: List[String])

object UserClosetModel {
    implicit val schema: zio.schema.Schema.CaseClass2[String, List[String], UserClosetModel] = zio.schema.DeriveSchema.gen[UserClosetModel]
    val (userId, closetItemKeys) = ProjectionExpression.accessors[UserClosetModel]
}
