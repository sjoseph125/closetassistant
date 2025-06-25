package core

final case class GetPresignedURLRequest(
    userId: String,
    urlType: PresignedUrlType = PresignedUrlType.PUT,
    numOfUrls: Option[Int] = None,
    closetItemKeys: List[String] = Nil
)

enum PresignedUrlType:
  case PUT
  case GET


