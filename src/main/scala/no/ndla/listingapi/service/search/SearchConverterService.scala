/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.listingapi.service.search

import com.typesafe.scalalogging.LazyLogging
import no.ndla.listingapi.model.domain.Cover
import no.ndla.listingapi.model.domain.search.{LanguageValue, SearchableCover, SearchableLanguageList, SearchableLanguageValues}

import scala.util.{Failure, Success}

trait SearchConverterService {
  val searchConverterService: SearchConverterService

  class SearchConverterService extends LazyLogging {
    def asSearchableCard(card: Cover): SearchableCover = {
      card.getSupportedLanguages match {
        case Failure(e) => throw e
        case Success(supportedLanguages) =>
          SearchableCover(
            id = card.id.get,
            title = SearchableLanguageValues(card.title.map(title => LanguageValue(title.language, title.title))),
            description = SearchableLanguageValues(card.description.map(description => LanguageValue(description.language, description.description))),
            card.articleApiId,
            card.coverPhotoUrl,
            SearchableLanguageList(card.labels.map(label => LanguageValue(label.language, label.labels))),
            supportedLanguages
          )
      }
    }
  }
}
