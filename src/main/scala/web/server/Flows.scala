package web.server

import zio._
import zio.http._
import zio.Task
import zio.aws.dynamodb.DynamoDb
import zio.aws.core.AwsError
import web.layers.ServiceLayers
import business.GetUserCloset.*
import core.UserCloset
import zio.dynamodb.{DynamoDBExecutor, DynamoDBError}
import persistence.models.*
import zio.dynamodb.KeyConditionExpr
import persistence.queries.DynamoDBQueries
import business.GetUserCloset
import zio.dynamodb.DynamoDBQuery
import zio.stream.ZStream
import zio.dynamodb.DynamoDBError.ItemError

trait Flows {
  lazy val closetDataTableName = "arn:aws:dynamodb:us-east-1:288761768209:table/closet_data"
  lazy val closetItemTableName = "arn:aws:dynamodb:us-east-1:288761768209:table/closet_items"

  lazy val getClosetData: (KeyConditionExpr[UserClosetModel]) => ZIO[DynamoDBExecutor, Throwable, Chunk[UserClosetModel]] = 
    keyCondition => DynamoDBQueries.queryAll[UserClosetModel](closetDataTableName, keyCondition)
    
  lazy val getClosetItem: (KeyConditionExpr.PrimaryKeyExpr[ClosetItem]) => DynamoDBQuery[ClosetItem, Either[ItemError, ClosetItem]] = 
    keyCondition => DynamoDBQueries.get[ClosetItem](closetItemTableName)(keyCondition)

  lazy val getUserCloset: String => URIO[DynamoDBExecutor, Option[UserCloset]] = userId =>
    new GetUserCloset(
      GetUserCloset.CfgCtx(
        getClosetData = getClosetData,
        getClosetItem = getClosetItem
      )
    )(userId)
      
}

// class FlowsBoot extends Flows {

//   override def getUserCloset: String => Task[String] = userId =>
//     new GetUserCloset().apply(userId)
//         // .map(userId => Response.text(s"Greeting user with ID: $userId"))
//         // .catchAll(err => Zio.succeed(Response.text(s"Error: ${err.getMessage}")))
// }
