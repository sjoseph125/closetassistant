package business

import business.GetPresignedURLSvcFlow.*
import zio.dynamodb.KeyConditionExpr
import persistence.models.UserClosetModel
import zio.dynamodb.DynamoDBExecutor
import zio.*
import zio.aws.s3.S3
import zio.http.URL
import software.amazon.awssdk.services.s3.presigner.{S3Presigner, model}
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import scala.util.chaining.*
import java.util.UUID

class GetPresignedURLSvcFlow(cfgCtx: CfgCtx)
    extends (String => ZIO[S3Presigner & DynamoDBExecutor, Exception, URL]) {
  import cfgCtx._
  override def apply(
      userId: String
  ): ZIO[S3Presigner & DynamoDBExecutor, Exception, URL] = {
    ZIO.logInfo(s"Starting GetPresignedURL flow for user $userId")
    getClosetData(
      UserClosetModel.userId.partitionKey === userId
    ).foldZIO(
      err =>
        ZIO.logError(
          s"Error fetching closet data for user $userId: ${err.getMessage}"
        ) *> ZIO.fail(new Exception("Failed to fetch closet data")),
      result => {
        if (result.isEmpty) {
          ZIO.logInfo(s"No closet found for user $userId") *> ZIO.fail(
            new Exception("No closet found")
          )
        } else {
          ZIO.logInfo(
            s"Found closet for user $userId with items: ${result.head.closetItemKeys.mkString(", ")}"
          )
          ZIO.serviceWith[S3Presigner](presigner =>
            constructPresignedUrl(presigner, userId, result.headOption)
          )
        }
      }
    )
  }

  def constructPresignedUrl(
      presigner: S3Presigner,
      userId: String,
      resultOpt: Option[UserClosetModel]
  ): URL = {
    resultOpt
      .flatMap { result =>
        PutObjectRequest
          .builder()
          .bucket(bucketName)
          .key(s"${result.imageRepoId}/${UUID.randomUUID().toString()}")
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
            URL
              .decode(presigner.presignPutObject(presignRequest).url().toString)
              .toOption
          )
      }
      .getOrElse(URL.empty)
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
