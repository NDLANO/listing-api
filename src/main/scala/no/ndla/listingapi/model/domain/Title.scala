package no.ndla.listingapi.model.domain

case class Title(title: String,
                 language: String) extends LanguageField[String] {
  def data = title
}
