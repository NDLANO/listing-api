/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.listingapi.service.search

import java.util.Date

import no.ndla.listingapi.model.api.NotFoundException
import no.ndla.listingapi.model.domain.{Description, Label, LanguageLabels, Title}
import no.ndla.listingapi.model.domain.search.LanguageValue.LanguageValue
import no.ndla.listingapi.model.domain.search.{SearchableCover, SearchableLanguageList, SearchableLanguageValues}
import no.ndla.listingapi.{TestData, TestEnvironment, UnitSuite}

class SearchConverterServiceTest extends UnitSuite with TestEnvironment {

  override val searchConverterService = new SearchConverterService
  val sampleCover = TestData.sampleCover.copy(
    title = Seq(Title("title", Some("nb"))),
    description = Seq(Description("description", Some("nb"))),
    labels = Seq(LanguageLabels(Seq(Label(None, Seq("label"))), Some("nb")))
  )

  test("asSearchableCard should convert to expected SearchableCover") {
    val expected = SearchableCover(
      sampleCover.id.get,
      sampleCover.revision.get,
      SearchableLanguageValues(Seq(LanguageValue(Some("nb"), "title"))),
      SearchableLanguageValues(Seq(LanguageValue(Some("nb"), "description"))),
      sampleCover.articleApiId,
      sampleCover.coverPhotoUrl,
      SearchableLanguageList(Seq(LanguageValue(Some("nb"), sampleCover.labels.head.labels))),
      Seq("nb"),
      sampleCover.updatedBy,
      TestData.updated(),
      sampleCover.theme
    )

    searchConverterService.asSearchableCover(sampleCover) should equal (expected)
  }

  test("asSearchableCard should throw a NotFoundException if cover contains incomplete data for a language") {
    assertThrows[NotFoundException] {
      searchConverterService.asSearchableCover(sampleCover.copy(title=Seq.empty))
    }
  }

}
