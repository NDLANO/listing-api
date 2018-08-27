package no.ndla.listingapi.model.domain

case class LanguageLabels(labels: Seq[Label], language: String)
    extends LanguageField[Seq[Label]] {
  def data = labels
}
