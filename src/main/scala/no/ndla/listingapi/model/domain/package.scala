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

  def emptySomeToNone(lang: Option[String]): Option[String] = lang.filter(_.nonEmpty)

  trait LanguageField[T] {
    val language: Option[String]
    def data: T
  }

  def getByLanguage[T, U <: LanguageField[T]](fields: Seq[U], language: String): Option[T] =
    fields.find(_.language.getOrElse("unknown") == language).map(_.data)
}
