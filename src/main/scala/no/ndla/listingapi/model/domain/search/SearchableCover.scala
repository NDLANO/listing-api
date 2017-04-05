/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.listingapi.model.domain.search

import no.ndla.listingapi.model.domain.{Label, emptySomeToNone}

object LanguageValue {
  case class LanguageValue[T](lang: Option[String], value: T)

  def apply[T](lang: Option[String], value: T): LanguageValue[T] = LanguageValue(emptySomeToNone(lang), value)
}

case class SearchableLanguageValues(languageValues: Seq[LanguageValue.LanguageValue[String]])
case class SearchableLanguageList(languageValues: Seq[LanguageValue.LanguageValue[Seq[Label]]])

case class SearchableCover(
  id: Long,
  title: SearchableLanguageValues,
  description: SearchableLanguageValues,
  articleApiId: Long,
  coverPhotoUrl: String,
  labels: SearchableLanguageList,
  supportedLanguages: Seq[String]
)