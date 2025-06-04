package core

import zio.json.JsonEncoder
import persistence.models.ClosetItemModel
import zio.json.JsonDecoder

final case class UserCloset(
    userId: String,
    numOfItems: Int,
    closetItems: List[ClosetItemModel] = List.empty
) derives JsonEncoder
