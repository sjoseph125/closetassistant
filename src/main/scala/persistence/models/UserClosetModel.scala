package persistence.models

import zio.schema.*
import zio.dynamodb.ProjectionExpression
import zio.schema.{Schema, DeriveSchema}

final case class UserClosetModel(
    userId: String, 
    closetItemKeys: List[String] = List.empty, 
    imageRepoId: String)

object UserClosetModel {
    implicit val schema: Schema.CaseClass3[String, List[String], String, UserClosetModel] = DeriveSchema.gen[UserClosetModel]
    val (userId, closetItemKeys, imageRepoId) = ProjectionExpression.accessors[UserClosetModel]
}
