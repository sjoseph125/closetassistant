package business

import business.UpdateUserClosetSvcFlow._
import core.*
import persistence.models.*
import persistence.models.ClosetItemModel.closetItemKey
import scala.util.chaining.scalaUtilChainingOps
import zio.*
import zio.dynamodb.*
import zio.dynamodb.DynamoDBExecutor
import zio.dynamodb.UpdateExpression.Action
import zio.dynamodb.KeyConditionExpr.PrimaryKeyExpr
import zio.aws.s3.S3
import web.layers.ServiceLayers.*
import zio.http.{Response, Client}
import persistence.queries.DynamoDBQueries.update
import persistence.models.ClosetItemModel.itemMetadata
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.util.UUID
import zio.http.Body
import zio.http.ZClient

class UpdateUserClosetSvcFlow(cfgCtx: CfgCtx)
    extends (
        UpdateUserCloset => RIO[ClientAndS3 & ExecutorAndPresignerType, Option[
          UserCloset
        ]]
    ) {
  import cfgCtx._

  override def apply(
      updateUserCloset: UpdateUserCloset
  ): RIO[ClientAndS3 & ExecutorAndPresignerType, Option[UserCloset]] = {
    import updateUserCloset.*
    println(
      s"Starting UpdateUserCloset flow for user ${updateUserCloset.userId}"
    )
    getClosetData(
      UserClosetModel.userId.partitionKey === userId
    ).foldZIO(
      err =>
        println(
          s"Error fetching closet data for user $userId: ${err.getMessage}"
        )
        ZIO.fail(new Exception("Failed to fetch closet data")),
      result =>
        result.headOption match {
          case None =>
            println(s"No closet found for user $userId")
            createNewCloset(updateUserCloset)

          case Some(result) =>
            println(
              s"Found closet for user $userId with ${result.closetItemKeys.size} items"
            )

            detemineAddorDelete(
              updateUserCloset,
              result.closetItemKeys,
              closetItemKeys,
              result.imageRepoId
            )
        }
    )
  }

  private def createNewCloset(updateUserCloset: UpdateUserCloset): RIO[ClientAndS3 & ExecutorAndPresignerType, Option[UserCloset]] = {
    println(s"Creating new closet for user ${updateUserCloset.userId}")
    addNewCloset(
      UserClosetModel(
        userId = updateUserCloset.userId,
        closetItemKeys = updateUserCloset.closetItemKeys,
        imageRepoId = UUID.randomUUID().toString
      )
    ).execute
      .flatMap(newUserCloset => detemineAddorDelete(
        updateUserCloset,
        newUserCloset.toList.flatMap(_.closetItemKeys),
        updateUserCloset.closetItemKeys,
        newUserCloset.map(_.imageRepoId).getOrElse(UUID.randomUUID().toString)
      ))
  }

  private def detemineAddorDelete(
      updateUserCloset: UpdateUserCloset,
      existingClosetItemKeys: List[String],
      closetItemKeys: List[String],
      imageRepoId: String
  ): RIO[ClientAndS3 & ExecutorAndPresignerType, Option[UserCloset]] = {
    if (updateUserCloset.deleteItems) deleteClosetItems(closetItemKeys)
    else {
      runLLMInference(imageRepoId, closetItemKeys)
      // .foldZIO(
      //   cause =>
      //     ZIO.logError(
      //       s"Error running LLM inference for user ${updateUserCloset.userId}: ${cause.getMessage}"
      //     ) *>
      //       ZIO.fail(new Exception("Failed to run LLM inference")),
      //   llmResponse =>
      //     addNewClosetItem(closetItemKeys, llmResponse)
      // )
    }

  }.foldZIO(
    err =>
      println(
        s"Error updating closet items: ${err.getMessage}"
      )
      ZIO.fail(new Exception("Failed to add closet items")),
    res =>
      println(
        s"Updated closet itmes for user ${updateUserCloset.userId} with items: ${closetItemKeys
            .mkString(", ")} " +
          s"and action: ${if (updateUserCloset.deleteItems) "delete" else "add"}"
      )
      updateClosetDataWithItems(
        updateUserCloset.userId,
        closetItemKeys,
        existingClosetItemKeys,
        deleteItems = updateUserCloset.deleteItems
      )
  )

  private def runLLMInference(
      imageRepoId: String,
      closetItemKeys: List[String]
  ): RIO[ClientAndS3 & DynamoDBExecutor, List[Option[ClosetItemModel]]] = {
    println(
      s"Running LLM inference for closet items: ${closetItemKeys.mkString(", ")}"
    )
    llmInferenceFlow(PerformInference(imageRepoId, closetItemKeys))
      .flatMap(llmResponse => addNewClosetItem(closetItemKeys, llmResponse))
      .retry(Schedule.once.addDelay(_ => Duration.fromMillis(1000L)))
  }

  private def addNewClosetItem(
      closetItemKeys: List[String],
      llmResponse: Map[String, LLMInferenceResponse]
  ): ZIO[DynamoDBExecutor, DynamoDBError, List[Option[ClosetItemModel]]] =
    DynamoDBQuery
      .forEach(closetItemKeys) { key =>
        addClosetItem(
          ClosetItemModel(
            closetItemKey = key,
            itemType = llmResponse.get(key).flatMap(_.responseAddItem.map(_.category)),
            itemMetadata = llmResponse.get(key).flatMap(_.responseAddItem)
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
  ): ZIO[ExecutorAndPresignerType, DynamoDBError, Option[UserCloset]] =

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
      ).flatMap(_ => getUserCloset(userId, false))
    )

}

object UpdateUserClosetSvcFlow {
  case class CfgCtx(
      getClosetData: KeyConditionExpr[UserClosetModel] => RIO[
        DynamoDBExecutor,
        Chunk[UserClosetModel]
      ],
      addClosetItem: ClosetItemModel => DynamoDBQuery[ClosetItemModel, Option[
        ClosetItemModel
      ]],
      addNewCloset: UserClosetModel => DynamoDBQuery[UserClosetModel, Option[UserClosetModel]],
      updateClosetData: (
          PrimaryKeyExpr[UserClosetModel],
          Action[UserClosetModel]
      ) => ZIO[DynamoDBExecutor, DynamoDBError, Option[UserClosetModel]],
      getUserCloset: (String, Boolean) => URIO[ExecutorAndPresignerType, Option[
        UserCloset
      ]],
      deleteClosetItem: PrimaryKeyExpr[ClosetItemModel] => DynamoDBQuery[
        ClosetItemModel,
        Option[ClosetItemModel]
      ],
      llmInferenceFlow: PerformInference => RIO[
        ClientAndS3,
        Map[String, LLMInferenceResponse]
      ]
  )
}
