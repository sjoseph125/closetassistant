package business
import business.GetPresignedURLSvcFlow.*
import core.*
import persistence.models.UserClosetModel
import software.amazon.awssdk.services.s3.model.{GetObjectRequest, PutObjectRequest}
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.{GetObjectPresignRequest, PutObjectPresignRequest}
import web.layers.ServiceLayers.ExecutorAndPresignerType
import zio.*
import zio.dynamodb.{DynamoDBExecutor, KeyConditionExpr}
import java.util.UUID
import scala.util.chaining.*

class GetPresignedURLSvcFlow(cfgCtx: CfgCtx)
    extends (
        GetPresignedURLRequest => RIO[
          ExecutorAndPresignerType,
          GetPresignedURLResponse
        ]
    ) {
  import cfgCtx._
  override def apply(
      input: GetPresignedURLRequest
  ): RIO[ExecutorAndPresignerType, GetPresignedURLResponse] = {
    import input.*
    ZIO.logInfo(s"Starting GetPresignedURL flow for user ${userId}")
    getClosetData(
      UserClosetModel.userId.partitionKey === input.userId
    ).foldZIO(
      err =>
        ZIO.logError(
          s"Error fetching closet data for user ${userId}: ${err.getMessage}"
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
            ZIO
              .serviceWithZIO[S3Presigner] { presigner =>
                urlType match {
                  case PresignedUrlType.PUT =>
                    generatePutPresignedUrls(
                      presigner,
                      result.imageRepoId,
                      numOfUrls.getOrElse(throw new Exception("Invalid request")).toInt
                    )

                  case PresignedUrlType.GET =>
                    generateGetPresignedUrl(
                      presigner,
                      result.imageRepoId,
                      closetItemKeys
                    )
                }
              }
              .map(GetPresignedURLResponse(_))
        }
      }
    )
  }

  private def generatePutPresignedUrls(
      presigner: S3Presigner,
      imageRepoId: String,
      numOfUrls: Int
  ): UIO[List[PresignedURLs]] = {
    ZIO.logInfo(
      s"Generating ${numOfUrls} PUT presigned URLs."
    )
    ZIO
      .foreachPar(0 until numOfUrls) { _ =>
        val imageIdentifier = UUID.randomUUID().toString().replace("-", "")
        PutObjectRequest
          .builder()
          .bucket(bucketName)
          .key(s"${imageRepoId}/${imageIdentifier}")
          .build()
          .pipe(putObjectRequest =>
            PutObjectPresignRequest
              .builder()
              .putObjectRequest(putObjectRequest)
              .signatureDuration(
                Duration.fromMillis(300000L)
              ) // Set expiration time for the presigned URL
              .build()
          )
          .pipe(putPresignRequest =>
            PresignedURLs(
              imageIdentifier = imageIdentifier,
              presignedUrl =
                presigner.presignPutObject(putPresignRequest).url().toString()
            ).pipe(ZIO.succeed)
          )
      }
      .map(_.toList)
  }

  private def generateGetPresignedUrl(
      presigner: S3Presigner,
      imageRepoId: String,
      closetItemKeys: List[String]
  ): UIO[List[PresignedURLs]] = {
    ZIO.logInfo(
      s"Generating GET presigned URLs for keys ${closetItemKeys.mkString(", ")}"
    )
    ZIO.foreachPar(closetItemKeys) { key =>
      GetObjectRequest
        .builder()
        .bucket(bucketName)
        .key(s"$imageRepoId/$key")
        .build()
        .pipe(getObjectRequest =>
          GetObjectPresignRequest
            .builder()
            .getObjectRequest(getObjectRequest)
            .signatureDuration(Duration.fromMillis(300000L))
            .build()
        )
        .pipe(getPresignRequest =>
          PresignedURLs(
            imageIdentifier = key,
            presignedUrl =
              presigner.presignGetObject(getPresignRequest).url().toString()
          ).pipe(ZIO.succeed)
        )
    }
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
