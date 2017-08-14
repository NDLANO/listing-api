/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */
package no.ndla.listingapi.model

package object domain {
  type Lang = String
  type LabelType = String
  type LabelName = String
  type ThemeName = String

  def emptySomeToNone(maybeString: Option[String]): Option[String] = maybeString.filter(_.nonEmpty)

  trait LanguageField[T] {
    val language: String
    def data: T
  }

  def getByLanguage[T, U <: LanguageField[T]](fields: Seq[U], language: String): T =
    fields.find(_.language == language).map(_.data).getOrElse(throw new RuntimeException("missing language"))
}
