package business
import business.GetPresignedURLSvcFlow.*
import core.{GetPresignedURLRequest, GetPresignedURLResponse, PresignedUrlType, PresignedURLs}
import persistence.models.UserClosetModel
import software.amazon.awssdk.services.s3.model.{
  GetObjectRequest,
  PutObjectRequest
}
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.{
  GetObjectPresignRequest,
  PutObjectPresignRequest
}
import zio.*
import zio.dynamodb.{DynamoDBExecutor, KeyConditionExpr}
import java.util.UUID
import scala.util.chaining.*
import web.layers.ServiceLayers.ExecutorAndPresignerType

class GetPresignedURLSvcFlow(cfgCtx: CfgCtx) extends (GetPresignedURLRequest => RIO[ExecutorAndPresignerType, GetPresignedURLResponse]) {
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
            ) *>
              ZIO.serviceWithZIO[S3Presigner] { presigner =>
                ZIO.foreachPar(0 until numOfUrls) { _ =>
                    ZIO
                      .attempt(
                        constructPresignedUrl(
                          presigner,
                          userId,
                          result,
                          urlType
                        )
                      )
                      .tap(url =>
                        ZIO.logInfo(
                          s"Generated presigned URL for item: ${url.imageIdentifier}"
                        )
                      )
                  }
                  .map(urls => GetPresignedURLResponse(urls.toList))
              }
        }
      }
    )
  }

  def constructPresignedUrl(
      presigner: S3Presigner,
      userId: String,
      result: UserClosetModel,
      urlType: PresignedUrlType
  ): PresignedURLs = {
    val imageIdentifier = UUID.randomUUID().toString().replace("-", "")
    {
      urlType match {
        case PresignedUrlType.PUT =>
          generatePutPresignedUrl(presigner, result, imageIdentifier)
        case PresignedUrlType.GET =>
          generateGetPresignedUrl(presigner, result, imageIdentifier)
      }
    }.pipe(presignedUrl =>
      PresignedURLs(
        imageIdentifier,
        presignedUrl
      )
    )
  }

  private def generatePutPresignedUrl(
      presigner: S3Presigner,
      result: UserClosetModel,
      imageIdentifier: String
  ): String = {
    PutObjectRequest
      .builder()
      .bucket(bucketName)
      .key(s"${result.imageRepoId}/${imageIdentifier}")
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
        presigner.presignPutObject(putPresignRequest).url().toString()
      )
  }

  private def generateGetPresignedUrl(
      presigner: S3Presigner,
      result: UserClosetModel,
      imageIdentifier: String
  ): String = {
    GetObjectRequest
      .builder()
      .bucket(bucketName)
      .key(s"${result.imageRepoId}/${imageIdentifier}")
      .build()
      .pipe(getObjectRequest =>
        GetObjectPresignRequest
          .builder()
          .getObjectRequest(getObjectRequest)
          .signatureDuration(Duration.fromMillis(300000L))
          .build()
      )
      .pipe(getPresignRequest =>
        presigner.presignGetObject(getPresignRequest).url().toString()
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
