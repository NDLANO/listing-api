package no.ndla.listingapi.model.domain

case class LanguageLabels(labels: Seq[Label], language: Option[String]) extends LanguageField[Seq[Label]] {
  def data = labels
}
