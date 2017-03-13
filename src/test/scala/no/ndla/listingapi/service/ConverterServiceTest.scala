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

  val sampleCard: domain.Card = TestData.sampleCard

  test("That toApiCard converts a domain class to an api class") {
    val expected = api.Card(sampleCard.id.get, sampleCard.coverPhotoUrl, sampleCard.title, sampleCard.description, sampleCard.articleId,
      Seq(api.Label(Some("kategori"), Seq("personlig verktøy")), api.Label(None, Seq("bygg"))))
    service.toApiCard(sampleCard) should equal (expected)
  }

}
