package no.ndla.listingapi.model.domain

case class Description(description: String, language: Option[String]) extends LanguageField[String] { def data = description }
