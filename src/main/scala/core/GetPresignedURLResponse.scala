package core

import zio.json.JsonEncoder

final case class GetPresignedURLResponse(
    presignedUrls: List[PresignedURLs]
) derives JsonEncoder

final case class PresignedURLs(
    imageIdentifier: String,
    presignedUrl: String
) derives JsonEncoder
