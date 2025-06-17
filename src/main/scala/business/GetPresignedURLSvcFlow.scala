package business

import business.GetPresignedURLSvcFlow.*
import core.GetPresignedURL
import persistence.models.UserClosetModel
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.{S3Presigner, model}
import zio.*
import zio.dynamodb.{DynamoDBExecutor, KeyConditionExpr}
import java.util.UUID
import scala.util.chaining.*
import web.layers.ServiceLayers.ExecutorAndPresignerType

class GetPresignedURLSvcFlow(cfgCtx: CfgCtx)
    extends (String => RIO[ExecutorAndPresignerType, GetPresignedURL]) {
  import cfgCtx._
  override def apply(
      userId: String
  ): ZIO[ExecutorAndPresignerType, Exception, GetPresignedURL] = {
    ZIO.logInfo(s"Starting GetPresignedURL flow for user $userId")
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
              s"Found closet for user $userId with items: ${result.closetItemKeys.mkString(", ")}"
            )
            ZIO.serviceWith[S3Presigner](presigner =>
              constructPresignedUrl(presigner, userId, result)
              .tap(
                url => ZIO.logInfo(s"Generated presigned URL for item: ${url.imageIdentifier}")
              )
            )
        }
      }
    )
  }

  def constructPresignedUrl(
      presigner: S3Presigner,
      userId: String,
      result: UserClosetModel
  ): GetPresignedURL = {
    val imageIdentifier = UUID.randomUUID().toString().replace("-", "")
    PutObjectRequest
      .builder()
      .bucket(bucketName)
      .key(s"${result.imageRepoId}/${imageIdentifier}")
      .build()
      .pipe(putObjectRequest =>
        model.PutObjectPresignRequest
          .builder()
          .putObjectRequest(putObjectRequest)
          .signatureDuration(
            Duration.fromMillis(300000L)
          ) // Set expiration time for the presigned URL
          .build()
      )
      .pipe(presignRequest =>
        GetPresignedURL(
          imageIdentifier,
          presigner.presignPutObject(presignRequest).url().toString()
        )        
      )
  }
}

object GetPresignedURLSvcFlow {
  case class CfgCtx(
      getClosetData: KeyConditionExpr[UserClosetModel] => ZIO[
        DynamoDBExecutor,
        Throwable,
        Chunk[UserClosetModel]
      ],
      bucketName: String
  )
}
