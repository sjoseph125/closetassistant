package business

import business.UpdateUserClosetSvcFlow._
import core.{UpdateUserCloset, UserCloset}
import persistence.models.*
import persistence.models.ClosetItemModel.closetItemKey
import scala.util.chaining.scalaUtilChainingOps
import zio.*
import zio.dynamodb.*
import zio.dynamodb.DynamoDBExecutor
import zio.dynamodb.UpdateExpression.Action
import zio.dynamodb.KeyConditionExpr.PrimaryKeyExpr

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
            detemineAddorDelete(
              updateUserCloset,
              result.closetItemKeys,
              closetItemKeys
            )
        }
      }
    )
  }

  private def detemineAddorDelete(
      updateUserCloset: UpdateUserCloset,
      existingClosetItemKeys: List[String],
      closetItemKeys: List[String]
  ): RIO[DynamoDBExecutor, Option[UserCloset]] = {
    {
      if (updateUserCloset.deleteItems) deleteClosetItems(closetItemKeys)
      else addNewClosetItem(closetItemKeys)

    }.foldZIO(
      err =>
        ZIO.logError(
          s"Error updating closet items: ${err.getMessage}"
        ) *>
          ZIO.fail(new Exception("Failed to add closet items")),
      res =>
        ZIO.logInfo(
          s"Updated closet itmes for user ${updateUserCloset.userId} with items: ${closetItemKeys
              .mkString(", ")} " +
            s"and action: ${
                if (updateUserCloset.deleteItems) "delete" else "add"
              }"
        )
        updateClosetDataWithItems(
          updateUserCloset.userId,
          closetItemKeys,
          existingClosetItemKeys,
          deleteItems = updateUserCloset.deleteItems
        )
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

  private def deleteClosetItems(
      closetItemKeys: List[String]
  ): ZIO[DynamoDBExecutor, DynamoDBError, List[ClosetItemModel]] =
    ZIO.logInfo(s"Deleting closet items: ${closetItemKeys.mkString(", ")}")

    DynamoDBQuery
      .forEach(closetItemKeys) { key =>
        deleteClosetItem(ClosetItemModel.closetItemKey.partitionKey === key)
      }
      .execute
      .map(_.flatten)

  private def updateClosetDataWithItems(
      userId: String,
      closetItemKeys: List[String],
      existingClosetItemKeys: List[String],
      deleteItems: Boolean
  ): ZIO[DynamoDBExecutor, DynamoDBError, Option[UserCloset]] =

    ZIO.logInfo(
      s"Updating closet items: ${closetItemKeys.mkString(", ")}, deleteItems: $deleteItems"
    )

    {
      if (deleteItems) {
        existingClosetItemKeys diff closetItemKeys
      } else {
        existingClosetItemKeys ++: closetItemKeys
      }
    }.pipe(updatedItemKeys =>
      updateClosetData(
        UserClosetModel.userId.partitionKey === userId,
        UserClosetModel.closetItemKeys.set(
          updatedItemKeys.distinct
        )
      ).flatMap(_ => getUserCloset(userId))
    )

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
      ) => ZIO[DynamoDBExecutor, DynamoDBError, Option[UserClosetModel]],
      getUserCloset: String => URIO[DynamoDBExecutor, Option[UserCloset]],
      deleteClosetItem: PrimaryKeyExpr[ClosetItemModel] => DynamoDBQuery[
        ClosetItemModel,
        Option[ClosetItemModel]
      ]
  )
}
