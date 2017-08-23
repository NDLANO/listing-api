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

  def emptySomeToNone(lang: Option[String]): Option[String] = lang.filter(_.nonEmpty)

  trait LanguageField[T] {
    val language: String
    def data: T
  }

  def getByLanguage[U <: LanguageField[_]](fields: Seq[U], language: String): Option[U] =
    fields.find(_.language == language)
}
