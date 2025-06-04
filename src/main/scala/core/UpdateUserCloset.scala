package core

import persistence.models.ClosetItemModel
import zio.json.{DeriveJsonDecoder, JsonDecoder}

final case class UpdateUserCloset(
    userId: String,
    closetItems: List[ClosetItemModel]
) 

object UpdateUserCloset {
  // Make sure to import or define an implicit decoder for ClosetItem
  import persistence.models.ClosetItemModel


  implicit val decoder: JsonDecoder[UpdateUserCloset] = DeriveJsonDecoder.gen[UpdateUserCloset]
}
