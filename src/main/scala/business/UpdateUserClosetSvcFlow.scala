package business

import business.UpdateUserClosetSvcFlow._
import core.UpdateUserCloset
import zio.*
import zio.dynamodb.*
import zio.dynamodb.DynamoDBExecutor
import zio.dynamodb.DynamoDBError
import zio.dynamodb.DynamoDBQuery
import zio.dynamodb.KeyConditionExpr
import zio.dynamodb.KeyConditionExpr.PrimaryKeyExpr
import persistence.models.*

import core.UserCloset
import zio.dynamodb.DynamoDBError.ItemError
import zio.dynamodb.UpdateExpression.Action

class UpdateUserClosetSvcFlow(cfgCtx: CfgCtx) extends (UpdateUserCloset => URIO[DynamoDBExecutor, Option[UserCloset]]) {
  import cfgCtx._

  override def apply(updateUserCloset: UpdateUserCloset): URIO[DynamoDBExecutor, Option[UserCloset]] = {
    ZIO.logInfo(s"Starting UpdateUserCloset flow for user ${updateUserCloset.userId}")

    // Here you would implement the logic to update the user's closet
    // For example, you might fetch the existing closet, modify it, and save it back

    ZIO.logInfo(s"Updated closet for user ${updateUserCloset.userId} with items: ${updateUserCloset.closetItems.mkString(", ")}")
    ZIO.succeed(None)
  }
  
}

object UpdateUserClosetSvcFlow {
    case class CfgCtx(
      getClosetData: KeyConditionExpr[UserClosetModel] => ZIO[DynamoDBExecutor, Throwable, Chunk[UserClosetModel]],
      addClosetItem: (String, PrimaryKeyExpr[ClosetItemModel], Action[ClosetItemModel]) => DynamoDBQuery[ClosetItemModel, Option[ClosetItemModel]],
    )
}
