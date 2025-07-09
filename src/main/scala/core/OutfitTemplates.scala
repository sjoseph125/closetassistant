package core

import zio.json.JsonEncoder
import persistence.models.ClosetItemModel

object OutfitTemplates {


  final case class BasicTemplate(
    outerwear: Option[ClosetItemModel] = None,
    top: ClosetItemModel,
    bottom: ClosetItemModel,
    shoes: ClosetItemModel
  )  derives JsonEncoder

  final case class DressTemplate(
    dress: ClosetItemModel,
    shoes: ClosetItemModel
  ) derives JsonEncoder

  final case class LayeredTemplate(
    top: ClosetItemModel,
    bottom: ClosetItemModel,
    shoes: ClosetItemModel,
    outerwear: ClosetItemModel
  )
}
