/*
 * Part of NDLA listing_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.listingapi.service

import no.ndla.listingapi.model.{api, domain}
import no.ndla.listingapi.{TestData, TestEnvironment, UnitSuite}

import scala.util.Success

class ConverterServiceTest extends UnitSuite with TestEnvironment {
  val service = new ConverterService

  val sampleCover: domain.Cover = TestData.sampleCover

  test("That toApiCover converts a domain class to an api class") {
    val expected = api.Cover(sampleCover.id.get,
      sampleCover.revision.get,
      sampleCover.coverPhotoUrl,
      api.CoverTitle(sampleCover.title.head.title, sampleCover.title.head.language),
      api.CoverDescription(sampleCover.description.head.description, sampleCover.description.head.language),
      sampleCover.articleApiId,
      api.CoverLabels(Seq(api.Label(Some("kategori"), Seq("personlig verktøy", "bygg verktøy")), api.Label(None, Seq("bygg"))), "nb"),
      Set("nb"),
      "content-import-client",
      sampleCover.updated,
      sampleCover.theme,
      createOembedUrl(sampleCover.oldNodeId)
    )
    service.toApiCover(sampleCover, "nb") should equal (expected)
  }

}
