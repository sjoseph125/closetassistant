package business

import business.GetPresignedURLSvcFlow.*
import core.GetPresignedURL
import persistence.models.UserClosetModel
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.{S3Presigner, model}
import zio.*
import zio.Chunk
import zio.dynamodb.{DynamoDBExecutor, KeyConditionExpr}
import java.util.UUID
import scala.util.chaining.*

class GetPresignedURLSvcFlow(cfgCtx: CfgCtx)
    extends (String => ZIO[S3Presigner & DynamoDBExecutor, Exception, GetPresignedURL]) {
  import cfgCtx._
  override def apply(
      userId: String
  ): ZIO[S3Presigner & DynamoDBExecutor, Exception, GetPresignedURL] = {
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
            Duration.fromMillis(30000L)
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

  // Define any additional methods or types needed for the service flow
}
