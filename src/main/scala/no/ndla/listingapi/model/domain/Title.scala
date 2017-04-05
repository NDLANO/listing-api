package no.ndla.listingapi.model.domain

case class Title(title: String, language: Option[String]) extends LanguageField[String] { def data = title }
