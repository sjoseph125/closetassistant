package persistence.queries
import zio.*
import zio.schema.Schema
import zio.dynamodb.DynamoDBQuery
import zio.dynamodb.syntax.*
import persistence.models.UserClosetModel
import zio.dynamodb.KeyConditionExpr
import zio.dynamodb.KeyConditionExpr.PrimaryKeyExpr
import zio.dynamodb.DynamoDBError
import zio.dynamodb.DynamoDBExecutor
import zio.stream.ZStream
import zio.dynamodb.DynamoDBError.ItemError
import zio.aws.dynamodb.model.primitives.ProjectionExpression
import zio.aws.dynamodb.model.AttributeValue
import zio.aws.dynamodb.model.primitives.StringAttributeValue
import zio.dynamodb.AttrMap

object DynamoDBQueries {
  def get[From: Schema](tableName: String)(
      key: KeyConditionExpr.PrimaryKeyExpr[From]
  ): DynamoDBQuery[From, Either[ItemError, From]] =
    DynamoDBQuery.get(tableName)(key)

  def scanAll[From: Schema](
      tableName: String
  ): ZIO[DynamoDBExecutor, Throwable, Chunk[From]] =
    DynamoDBQuery
      .scanAll(tableName)
      .execute
      .flatMap(_.runCollect)

  def queryAll[From: Schema](
      tableName: String,
      keyCondition: KeyConditionExpr[From]
  ): ZIO[DynamoDBExecutor, Throwable, Chunk[From]] =
    DynamoDBQuery
      .queryAll(tableName)
      .whereKey(keyCondition)
      .execute
      .flatMap(_.runCollect)

}
