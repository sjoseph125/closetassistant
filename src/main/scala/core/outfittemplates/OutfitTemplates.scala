package core.outfittemplates

object OutfitTemplates {
  trait Template

  final case class BasicTemplate(
    top: String,
    bottom: String,
    shoes: String
  ) extends Template

  final case class DressTemplate(
    dress: String,
    shoes: String
  ) extends Template

  final case class LayeredTemplate(
    top: String,
    bottom: String,
    shoes: String,
    outerwear: String
  ) extends Template
}
