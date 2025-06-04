package web.server

import zio._
import zio.http._
import zio.Task
import zio.aws.dynamodb.DynamoDb
import zio.aws.core.AwsError
import web.layers.ServiceLayers
import core.UserCloset
import zio.dynamodb.{DynamoDBExecutor, DynamoDBError}
import persistence.models.*
import zio.dynamodb.KeyConditionExpr
import persistence.queries.DynamoDBQueries
import zio.dynamodb.DynamoDBQuery
import zio.stream.ZStream
import zio.dynamodb.DynamoDBError.ItemError
import core.*
import zio.dynamodb.KeyConditionExpr.PrimaryKeyExpr
import zio.dynamodb.UpdateExpression.Action
import zio.aws.s3.S3
import business.*
import software.amazon.awssdk.services.s3.presigner.S3Presigner

trait Flows {
  lazy val closetDataTableName = "arn:aws:dynamodb:us-east-1:288761768209:table/closet_data"
  lazy val closetItemTableName = "arn:aws:dynamodb:us-east-1:288761768209:table/closet_items"
  lazy val bucketName = "closet-assistant-image-repository" // Replace with your actual S3 bucket name

  lazy val getClosetData: (KeyConditionExpr[UserClosetModel]) => ZIO[DynamoDBExecutor, Throwable, Chunk[UserClosetModel]] = 
    keyCondition => DynamoDBQueries.queryAll[UserClosetModel](closetDataTableName, keyCondition)
    
  lazy val getClosetItem: (KeyConditionExpr.PrimaryKeyExpr[ClosetItemModel]) => DynamoDBQuery[ClosetItemModel, Either[ItemError, ClosetItemModel]] = 
    keyCondition => DynamoDBQueries.get[ClosetItemModel](closetItemTableName)(keyCondition)

  lazy val addClosetItem: (String, PrimaryKeyExpr[ClosetItemModel], Action[ClosetItemModel]) => DynamoDBQuery[ClosetItemModel, Option[ClosetItemModel]] = 
    (userId, key, action) => DynamoDBQueries.update[ClosetItemModel](closetItemTableName, key, action)

  lazy val getUserCloset: String => URIO[DynamoDBExecutor, Option[UserCloset]] = userId =>
    new GetUserClosetSvcFlow(
      GetUserClosetSvcFlow.CfgCtx(
        getClosetData = getClosetData,
        getClosetItem = getClosetItem
      )
    )(userId)

  lazy val updateUserCloset: UpdateUserCloset => URIO[DynamoDBExecutor, Option[UserCloset]] = updateUserCloset =>
    new UpdateUserClosetSvcFlow(
      UpdateUserClosetSvcFlow.CfgCtx(
        getClosetData = getClosetData,
        addClosetItem = addClosetItem
      )
    )(updateUserCloset)

  lazy val getPresignedUrl: String => ZIO[S3Presigner & DynamoDBExecutor, Exception, URL] = userId =>
    new GetPresignedURLSvcFlow(
      GetPresignedURLSvcFlow.CfgCtx(
        getClosetData = getClosetData,
        bucketName = bucketName
      )
    )(userId)
}

// class FlowsBoot extends Flows {

//   override def getUserCloset: String => Task[String] = userId =>
//     new GetUserCloset().apply(userId)
//         // .map(userId => Response.text(s"Greeting user with ID: $userId"))
//         // .catchAll(err => Zio.succeed(Response.text(s"Error: ${err.getMessage}")))
// }
