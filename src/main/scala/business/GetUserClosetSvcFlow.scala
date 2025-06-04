package business

import zio.*
import zio.dynamodb.*
import zio.dynamodb.DynamoDBExecutor
import zio.dynamodb.DynamoDBError
import zio.dynamodb.DynamoDBQuery
import zio.dynamodb.KeyConditionExpr
import zio.dynamodb.KeyConditionExpr.PrimaryKeyExpr
import persistence.models.*
import business.GetUserClosetSvcFlow.*
import core.UserCloset
import zio.dynamodb.DynamoDBError.ItemError

class GetUserClosetSvcFlow(cfgCtx: CfgCtx)
    extends (String => URIO[DynamoDBExecutor, Option[UserCloset]]) {
  import cfgCtx._
  override def apply(userId: String): URIO[DynamoDBExecutor, Option[UserCloset]] = {
    ZIO.logInfo("Starting GetUserClosetSvcFlow")

    getClosetData(
      UserClosetModel.userId.partitionKey === userId
    ).foldZIO(
      err =>
        ZIO.logError(
          s"Error fetching closet data for user $userId: ${err.getMessage}"
        ) *> ZIO.succeed(None),
      result => {
        if (result.isEmpty) {
          ZIO.logInfo(s"Found no closet for user $userId") *> ZIO.succeed(None)
        } else {
          ZIO.logInfo(
            s"Fetched closet for user $userId: ${result.size} with items ${result.map(_.closetItemKeys).mkString(", ")}"
          ) *>
          getClosetItems(result.flatMap(_.closetItemKeys).toList)
            .foldZIO(
              err => ZIO.logError(s"Error fetching closet items: ${err.getMessage}") *> ZIO.succeed(None),
              items => ZIO.succeed(
                result.headOption.map { closet =>
                  UserCloset(
                    userId = closet.userId,
                    numOfItems = closet.closetItemKeys.size,
                    closetItems = items
                  )
                }
              )
            )
        }
      }
    )
  }

  private def getClosetItems(closetItemKeys: List[String]): ZIO[DynamoDBExecutor, DynamoDBError, List[ClosetItemModel]] =
    val closetItemsBatch: ZIO[DynamoDBExecutor, DynamoDBError, List[Either[ItemError, ClosetItemModel]]] =
      DynamoDBQuery.forEach(closetItemKeys) { key =>
      ZIO.logInfo(s"Processing closet item key: $key")
      getClosetItem(ClosetItemModel.closetItemKey.partitionKey === key)
      }.execute
    for {
      items <- closetItemsBatch
      _ <- ZIO.logInfo(s"Fetched ${items.size} closet items")
    } yield {
      items.flatMap {
        case Right(item) => Some(item)
        case Left(error) =>
          ZIO.logError(s"Error fetching closet item: ${error.getMessage}")
          None
      }
    }
  }

object GetUserClosetSvcFlow {
  case class CfgCtx(
      getClosetData: KeyConditionExpr[UserClosetModel] => ZIO[DynamoDBExecutor, Throwable, Chunk[UserClosetModel]],
      getClosetItem: KeyConditionExpr.PrimaryKeyExpr[ClosetItemModel] => DynamoDBQuery[ClosetItemModel, Either[ItemError, ClosetItemModel]]
  )
}
