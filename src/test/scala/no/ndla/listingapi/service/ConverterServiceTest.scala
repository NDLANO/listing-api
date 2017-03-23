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

class ConverterServiceTest extends UnitSuite with TestEnvironment {
  val service = new ConverterService

  val sampleCover: domain.Cover = TestData.sampleCover

  test("That toApiCover converts a domain class to an api class") {
    val expected = api.Cover(sampleCover.id.get, sampleCover.coverPhotoUrl, sampleCover.title, sampleCover.description, sampleCover.articleId,
      Seq(api.Label(Some("kategori"), Seq("personlig verktøy")), api.Label(None, Seq("bygg"))))
    service.toApiCover(sampleCover) should equal (expected)
  }

}
