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
import persistence.models.ClosetItemModel.closetItemKey

class UpdateUserClosetSvcFlow(cfgCtx: CfgCtx)
    extends (UpdateUserCloset => RIO[DynamoDBExecutor, Option[UserCloset]]) {
  import cfgCtx._

  override def apply(
      updateUserCloset: UpdateUserCloset
  ): RIO[DynamoDBExecutor, Option[UserCloset]] = {
    import updateUserCloset.*
    ZIO.logInfo(
      s"Starting UpdateUserCloset flow for user ${updateUserCloset.userId}"
    )
    getClosetData(
      UserClosetModel.userId.partitionKey === userId
    ).foldZIO(
      err =>
        ZIO.logError(
          s"Error fetching closet data for user $userId: ${err.getMessage}"
        ) *> ZIO.fail(new Exception("Failed to fetch closet data")),
      result => {
        result.headOption match {
          case None =>
            ZIO.logInfo(s"No closet found for user $userId") *> ZIO.fail(
              new Exception("No closet found")
            )
          case Some(result) =>
            ZIO.logInfo(
              s"Found closet for user $userId with ${result.closetItemKeys.size} items"
            )
            addNewClosetItem(closetItemKeys)
              .foldZIO(
                err =>
                  ZIO.logError(
                    s"Error adding closet items: ${err.getMessage}"
                  ) *>
                    ZIO.fail(new Exception("Failed to add closet items")),
                res =>
                  ZIO.logInfo(
                    s"Updated closet for user $userId with items: ${closetItemKeys.mkString(", ")}"
                  )
                  updateClosetDataWithNewItems(
                    userId,
                    closetItemKeys,
                    result.closetItemKeys
                  )
              )
        }
      }
    )
  }

  private def addNewClosetItem(closetItemKeys: List[String]) =
    DynamoDBQuery
      .forEach(closetItemKeys) { key =>
        addClosetItem(
          ClosetItemModel(
            closetItemKey = key,
            itemType = "some type"
          )
        )
      }
      .execute

  private def updateClosetDataWithNewItems(
    userId: String,
    closetItemKeys: List[String],
    existingClosetItemKeys: List[String]) =

    updateClosetData(
      UserClosetModel.userId.partitionKey === userId,
      UserClosetModel.closetItemKeys.set(
        existingClosetItemKeys ++: closetItemKeys
      )
    ).execute
      .flatMap(_ => getUserCloset(userId))
}

object UpdateUserClosetSvcFlow {
  case class CfgCtx(
      getClosetData: KeyConditionExpr[UserClosetModel] => ZIO[
        DynamoDBExecutor,
        Throwable,
        Chunk[UserClosetModel]
      ],
      addClosetItem: ClosetItemModel => DynamoDBQuery[ClosetItemModel, Option[
        ClosetItemModel
      ]],
      updateClosetData: (
          PrimaryKeyExpr[UserClosetModel],
          Action[UserClosetModel]
      ) => DynamoDBQuery[UserClosetModel, Option[UserClosetModel]],
      getUserCloset: String => URIO[DynamoDBExecutor, Option[UserCloset]]
  )
}
