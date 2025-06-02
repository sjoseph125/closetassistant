package persistence.models

import zio.dynamodb.ProjectionExpression

final case class User(id: String)
object User {
    implicit val schema: zio.schema.Schema.CaseClass1[String, User] = zio.schema.DeriveSchema.gen[User]
    val (userId) = ProjectionExpression.accessors[User]
}