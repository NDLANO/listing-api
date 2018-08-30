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

import scala.util.{Failure, Success}

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
      val sortedInnerHits = result.innerHits.toList
        .filter(ih => ih._2.total > 0)
        .sortBy {
          case (_, hit) => hit.max_score
        }
        .reverse

      val matchLanguage = sortedInnerHits.headOption.flatMap {
        case (_, innerHit) =>
          innerHit.hits
            .sortBy(hit => hit.score)
            .reverse
            .headOption
            .flatMap(hit => {
              hit.highlight.headOption.map(hl => {
                hl._1.split('.').filterNot(_ == "labels").last
              })
            })
      }

      matchLanguage match {
        case Some(lang) =>
          Some(lang)
        case _ =>
          val title = result.sourceAsMap.get("title")
          val titleMap = title.map(tm => {
            tm.asInstanceOf[Map[String, _]]
          })

          val languages = titleMap.map(title => title.keySet.toList)

          languages.flatMap(languageList => {
            languageList
              .sortBy(lang => {
                val languagePriority =
                  Language.languageAnalyzers.map(la => la.lang).reverse
                languagePriority.indexOf(lang)
              })
              .lastOption
          })
      }
    }
  }

}
