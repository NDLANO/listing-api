/*
 * Part of NDLA listing_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.listingapi

import com.zaxxer.hikari.HikariDataSource
import no.ndla.listingapi.auth.{Client, Role}
import no.ndla.listingapi.controller.{HealthController, ListingController}
import no.ndla.listingapi.integration._
import no.ndla.listingapi.repository.ListingRepository
import no.ndla.listingapi.service._
import no.ndla.listingapi.service.search.{IndexService, SearchConverterService, SearchIndexService, SearchService}
import org.scalatest.mockito.MockitoSugar._

trait TestEnvironment
    extends DataSource
    with ReadService
    with WriteService
    with CoverValidator
    with SearchService
    with Elastic4sClient
    with SearchIndexService
    with SearchConverterService
    with IndexService
    with ConverterService
    with ListingRepository
    with ListingController
    with HealthController
    with Clock
    with Client
    with Role {

  val dataSource = mock[HikariDataSource]
  val listingRepository = mock[ListingRepository]
  val readService = mock[ReadService]
  val writeService = mock[WriteService]
  val converterService = mock[ConverterService]
  val coverValidator = mock[CoverValidator]

  val resourcesApp = mock[ResourcesApp]
  val healthController = mock[HealthController]
  val listingController = mock[ListingController]

  val searchService = mock[SearchService]
  val e4sClient = mock[NdlaE4sClient]
  val searchIndexService = mock[SearchIndexService]
  val searchConverterService = mock[SearchConverterService]
  val indexService = mock[IndexService]

  val clock = mock[SystemClock]
  val authClient = mock[AuthClient]
  val authRole = new AuthRole

}
