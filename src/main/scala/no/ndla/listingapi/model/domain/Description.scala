package no.ndla.listingapi.model.domain

case class Description(description: String, language: String)
    extends LanguageField[String] { def data = description }
