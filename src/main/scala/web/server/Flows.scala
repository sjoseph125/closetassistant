package web.server

import zio.*
import zio.dynamodb.*
import zio.dynamodb.KeyConditionExpr.PrimaryKeyExpr
import zio.dynamodb.UpdateExpression.Action
import zio.dynamodb.DynamoDBError.ItemError
import zio.dynamodb.DynamoDBExecutor
import persistence.models.*
import persistence.queries.DynamoDBQueries
import core.*
import core.UserCloset
import business.*
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import web.server.DBFlows.*

trait Flows {
  lazy val closetDataTableName = "arn:aws:dynamodb:us-east-1:288761768209:table/closet_data"
  lazy val closetItemTableName = "arn:aws:dynamodb:us-east-1:288761768209:table/closet_items"
  lazy val bucketName = "closet-assistant-image-repository" // Replace with your actual S3 bucket name

  lazy val getUserCloset: String => URIO[DynamoDBExecutor, Option[UserCloset]] = userId =>
    new GetUserClosetSvcFlow(
      GetUserClosetSvcFlow.CfgCtx(
        getClosetData = getClosetData,
        getClosetItem = getClosetItem
      )
    )(userId)

  lazy val updateUserCloset: UpdateUserCloset => RIO[DynamoDBExecutor, Option[UserCloset]] = updateUserCloset =>
    new UpdateUserClosetSvcFlow(
      UpdateUserClosetSvcFlow.CfgCtx(
        getClosetData = getClosetData,
        addClosetItem = addClosetItem,
        updateClosetData = updateClosetData,
        getUserCloset = getUserCloset
      )
    )(updateUserCloset)

  lazy val getPresignedUrl: String => ZIO[S3Presigner & DynamoDBExecutor, Exception, GetPresignedURL] = userId =>
    new GetPresignedURLSvcFlow(
      GetPresignedURLSvcFlow.CfgCtx(
        getClosetData = getClosetData,
        bucketName = bucketName
      )
    )(userId)
}

object DBFlows extends Flows {
  lazy val getClosetData: (KeyConditionExpr[UserClosetModel]) => ZIO[DynamoDBExecutor, Throwable, Chunk[UserClosetModel]] = 
    (keyCondition) => DynamoDBQueries.queryAll[UserClosetModel](closetDataTableName, keyCondition)
    
  lazy val getClosetItem: (KeyConditionExpr.PrimaryKeyExpr[ClosetItemModel]) => DynamoDBQuery[ClosetItemModel, Either[ItemError, ClosetItemModel]] = 
    keyCondition => DynamoDBQueries.get[ClosetItemModel](closetItemTableName)(keyCondition)

  lazy val updateClosetData: (PrimaryKeyExpr[UserClosetModel], Action[UserClosetModel]) => DynamoDBQuery[UserClosetModel, Option[UserClosetModel]] = 
    (key, action) => DynamoDBQueries.update[UserClosetModel](closetDataTableName, key, action)
    
  lazy val addClosetItem: ClosetItemModel => DynamoDBQuery[ClosetItemModel, Option[ClosetItemModel]] = 
    item => DynamoDBQueries.put[ClosetItemModel](closetItemTableName, item)
}