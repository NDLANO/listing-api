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

trait SearchConverterService {
  val searchConverterService: SearchConverterService

  class SearchConverterService extends LazyLogging {
    def asSearchableCard(c: Cover): SearchableCover = {
      SearchableCover(
        id = c.id.get,
        title = SearchableLanguageValues(c.title.map(title => LanguageValue(title.language, title.title))),
        description = SearchableLanguageValues(c.description.map(description => LanguageValue(description.language, description.description))),
        c.articleApiId,
        c.coverPhotoUrl,
        labels = SearchableLanguageList(c.labels.map(label => LanguageValue(label.language, label.labels))))
    }
  }
}
