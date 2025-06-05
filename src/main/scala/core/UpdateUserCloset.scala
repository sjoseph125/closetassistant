package core

import zio.json.{DeriveJsonDecoder, JsonDecoder}

final case class UpdateUserCloset(
    userId: String,
    closetItemKeys: List[String]
) 

object UpdateUserCloset {

  implicit val decoder: JsonDecoder[UpdateUserCloset] = DeriveJsonDecoder.gen[UpdateUserCloset]
}
