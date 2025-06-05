package core

import zio.json.JsonEncoder

final case class GetPresignedURL(
    imageIdentifier: String,
    presignedUrl: String
) derives JsonEncoder
