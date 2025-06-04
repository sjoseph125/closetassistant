package persistence.queries
import zio.*
import zio.schema.Schema
import zio.dynamodb.{DynamoDBQuery, KeyConditionExpr, DynamoDBExecutor}
import zio.dynamodb.KeyConditionExpr.PrimaryKeyExpr
import zio.dynamodb.DynamoDBError.ItemError
import zio.dynamodb.UpdateExpression.Action

object DynamoDBQueries {
  def get[From: Schema](tableName: String)(
      key: KeyConditionExpr.PrimaryKeyExpr[From]
  ): DynamoDBQuery[From, Either[ItemError, From]] =
    DynamoDBQuery.get(tableName)(key)

  // def batchGetItems[From: Schema](
  //     tableName: String,
  //     keys: List[String],
  //     partitionKey: ProjectionExpression
  // ): ZIO[DynamoDBExecutor, Throwable, Chunk[Either[ItemError, From]]] =
  
  //     DynamoDBQuery.forEach(keys) { key =>
  //     ZIO.logInfo(s"Processing closet item key: $key")
  //     get[From](tableName)(From. === key)
  //     }.execute


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

  def update[From: Schema](
      tableName: String,
      key: PrimaryKeyExpr[From],
      action: Action[From]
  ): DynamoDBQuery[From, Option[From]] =
    DynamoDBQuery.update(tableName)(key)(action)
}
