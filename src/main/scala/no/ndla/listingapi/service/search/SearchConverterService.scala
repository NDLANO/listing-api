/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.listingapi.service.search

import com.sksamuel.elastic4s.http.search.SearchHit
import com.typesafe.scalalogging.LazyLogging
import no.ndla.listingapi.model.domain.Cover
import no.ndla.listingapi.model.domain.search._
import no.ndla.mapping.ISO639
import org.json4s.Formats
import org.json4s.native.JsonParser.parse

trait SearchConverterService {
  val searchConverterService: SearchConverterService

  class SearchConverterService extends LazyLogging {

    def asSearchableCover(card: Cover): SearchableCover = {
      val defaultTitle = card.title
        .sortBy(title => {
          val languagePriority =
            Language.languageAnalyzers.map(la => la.lang).reverse
          languagePriority.indexOf(title.language)
        })
        .lastOption

      SearchableCover(
        id = card.id.get,
        revision = card.revision.get,
        title = SearchableLanguageValues(
          card.title.map(title => LanguageValue(title.language, title.title))),
        description =
          SearchableLanguageValues(card.description.map(description =>
            LanguageValue(description.language, description.description))),
        card.articleApiId,
        card.coverPhotoUrl,
        SearchableLanguageList(card.labels.map(label =>
          LanguageValue(label.language, label.labels))),
        defaultTitle.map(_.title),
        card.supportedLanguages,
        card.updatedBy,
        card.updated,
        card.theme,
        card.oldNodeId
      )
    }

    def getLanguageFromHit(result: SearchHit): Option[String] = {
      def prioritizedLanguage(languages: List[String]) = {
        languages
          .sortBy(lang => {
            ISO639.languagePriority.reverse.indexOf(lang)
          })
          .lastOption
      }
      def keyToLanguage(keys: Iterable[String]): Option[String] = {
        val keyLanguages = keys.toList.flatMap(key =>
          key.split('.').toList match {
            case _ :: language :: _ => Some(language)
            case _                  => None
        })
        prioritizedLanguage(keyLanguages)
      }

      val highlightKeys: Option[Map[String, _]] = Option(result.highlight)
      val matchLanguage = keyToLanguage(highlightKeys.getOrElse(Map()).keys)

      matchLanguage match {
        case Some(lang) =>
          Some(lang)
        case _ =>
          implicit val formats: Formats = SearchableLanguageFormats.JSonFormats
          val langs = (parse(result.sourceAsString) \\ "supportedLanguages")
            .extract[List[String]]
          prioritizedLanguage(langs)
      }
    }
  }

}
