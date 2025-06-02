package core

import zio.json.JsonEncoder
import persistence.models.ClosetItem

final case class UserCloset(
    userId: String,
    numOfItems: Int,
    closetItems: List[ClosetItem] = List.empty
) derives JsonEncoder
