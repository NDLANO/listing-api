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
      sampleCover.title.head.title,
      sampleCover.description.head.description,
      sampleCover.articleApiId,
      Seq(api.Label(Some("kategori"),
        Seq("personlig verktøy", "bygg verktøy")),
        api.Label(None, Seq("bygg"))),
      Seq("nb"),
      "NDLA import script",
      sampleCover.updated
    )
    service.toApiCover(sampleCover, "nb") should equal (Success(expected))
  }

  test("toApiCover should return Failure if the cover is incomplete for a given language") {
    service.toApiCover(sampleCover.copy(title=Seq.empty), "nb").isFailure should equal (true)
    service.toApiCover(sampleCover.copy(description=Seq.empty), "nb").isFailure should equal (true)
    service.toApiCover(sampleCover.copy(description=Seq.empty), "junk").isFailure should equal (true)
  }

}
