package web.externalservices

import web.externalservices.S3DownloadFlow.*
import zio.*
import zio.aws.s3.*
import zio.aws.s3.S3.getObject
import zio.aws.s3.model.GetObjectRequest
import zio.aws.s3.model.primitives.{BucketName, ObjectKey}

class S3DownloadFlow(cfgCtx: CfgCtx)
    extends (String => URIO[S3, Chunk[Byte]]) {
  import cfgCtx.*
  override def apply(fileId: String): URIO[S3, Chunk[Byte]] = {
    val getObjectRequest = GetObjectRequest(
      bucket = BucketName(bucketName),
      key = ObjectKey(fileId)
    )

    {
      for {
        streamingResult <- ZIO.serviceWithZIO[S3](_.getObject(getObjectRequest))
        content <- streamingResult.output.runCollect
      } yield content
    }.foldCause(
      cause => {
        ZIO.logError(
          s"Failed to download file with id $fileId with cause: ${cause.dieOption.map(_.getMessage)}"
        )
        Chunk.empty[Byte]
      },
      content => {
        ZIO.logInfo(s"Successfully downloaded file with id $fileId")
        content
      }
    )
  }

}

object S3DownloadFlow {
  case class CfgCtx(
      bucketName: String
  )
}
