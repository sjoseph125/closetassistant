package core

import zio.json.{DeriveJsonDecoder, JsonDecoder}

final case class UpdateUserCloset(
    userId: String,
    closetItemKeys: List[String],
    deleteItems: Boolean = false
) derives JsonDecoder