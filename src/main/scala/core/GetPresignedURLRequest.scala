package core

final case class GetPresignedURLRequest(
    userId: String,
    urlType: PresignedUrlType = PresignedUrlType.PUT,
    numOfUrls: Int
)

enum PresignedUrlType:
  case PUT
  case GET


