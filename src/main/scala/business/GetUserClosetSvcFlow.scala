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
import core.GetPresignedURLRequest
import web.layers.ServiceLayers.ExecutorAndPresignerType
import core.GetPresignedURLResponse
import core.PresignedUrlType
import java.util.UUID
import web.layers.ServiceLayers.ClientAndS3

class GetUserClosetSvcFlow(cfgCtx: CfgCtx)
    extends ((String, Boolean) => RIO[ExecutorAndPresignerType, Option[UserCloset]]) {
  import cfgCtx._
  override def apply(
      userId: String,
      includeMetaData: Boolean = false
  ): URIO[ExecutorAndPresignerType, Option[UserCloset]] = {
    println("Starting GetUserClosetSvcFlow")

    getClosetData(
      UserClosetModel.userId.partitionKey === userId
    ).foldZIO(
      err =>
        println(
          s"Error fetching closet data for user $userId: ${err.getMessage}"
        )
        ZIO.succeed(None),
      result => {
        result.headOption match {

          case None =>
            println(s"No closet found for user $userId")
            createNewCloset(userId)
              .fold(
                err => {
                  println(s"Error creating new closet for user $userId: ${err.getMessage}")
                  throw new Exception("Failed to create new closet")
                },
                closetOpt => Some(
                  UserCloset(
                    userId = userId,
                    numOfItems = 0
                  )
                )
              )

          case Some(_) =>
            println(
              s"Found closet for user $userId with items: ${result.flatMap(_.closetItemKeys).mkString(", ")}"
            )
            val closetItemKeys = result.flatMap(_.closetItemKeys).toList
            getClosetItems(closetItemKeys)
              .zipWithPar(
                getPresignedUrls(
                  GetPresignedURLRequest(
                    userId = userId,
                    closetItemKeys = closetItemKeys,
                    urlType = PresignedUrlType.GET
                  )
                )
              ) { (getUserClosetRes, presignedUrls) =>
                result.headOption.map { closet =>
                  UserCloset(
                    userId = closet.userId,
                    numOfItems = closet.closetItemKeys.size,
                    closetItems = getUserClosetRes.map(item =>
                      item.copy(
                        presignedUrl = presignedUrls.presignedUrls
                          .withFilter(_.imageIdentifier == item.closetItemKey)
                          .map(_.presignedUrl)
                          .headOption,
                        itemMetadata = if(includeMetaData) item.itemMetadata else None,
                        itemName = item.itemMetadata.map(_.itemName)
                      )
                    )
                  )
                }

              }
              .fold(
                err =>
                  ZIO.logError(
                    s"Error fetching closet items: ${err.getMessage}"
                  )
                  None
                ,
                items => items
              )
        }
      }
    )
  }

  private def getClosetItems(
      closetItemKeys: List[String]
  ): ZIO[DynamoDBExecutor, DynamoDBError, List[ClosetItemModel]] =
    val closetItemsBatch =
      DynamoDBQuery
        .forEach(closetItemKeys) { key =>
          ZIO.logInfo(s"Processing closet item key: $key")
          getClosetItem(ClosetItemModel.closetItemKey.partitionKey === key)
        }
        .execute
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

  private def createNewCloset(userId: String): RIO[DynamoDBExecutor, Option[UserClosetModel]] = {
    println(s"Creating new closet for user ${userId}")
    addNewCloset(
      UserClosetModel(
        userId = userId,
        imageRepoId = UUID.randomUUID().toString
      )
    ).execute
  }
}

object GetUserClosetSvcFlow {
  case class CfgCtx(
      getClosetData: KeyConditionExpr[UserClosetModel] => ZIO[
        DynamoDBExecutor,
        Throwable,
        Chunk[UserClosetModel]
      ],
      getClosetItem: KeyConditionExpr.PrimaryKeyExpr[
        ClosetItemModel
      ] => DynamoDBQuery[ClosetItemModel, Either[ItemError, ClosetItemModel]],
      getPresignedUrls: GetPresignedURLRequest => RIO[
        ExecutorAndPresignerType,
        GetPresignedURLResponse
      ],
      addNewCloset: UserClosetModel => DynamoDBQuery[UserClosetModel, Option[UserClosetModel]]
  )
}
