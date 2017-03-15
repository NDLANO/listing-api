/*
 * Part of NDLA listing_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.listingapi

import javax.sql

import no.ndla.listingapi.controller.{HealthController, ListingController}
import no.ndla.listingapi.integration.DataSource
import no.ndla.listingapi.repository.ListingRepository
import no.ndla.listingapi.service.{ConverterService, ReadService}
import org.scalatest.mockito.MockitoSugar._

trait TestEnvironment
  extends DataSource
  with ReadService
  with ConverterService
  with ListingRepository
  with ListingController
  with HealthController
{
  val dataSource = mock[sql.DataSource]
  val listingRepository = mock[ListingRepository]
  val readService = mock[ReadService]
  val converterService = mock[ConverterService]

  val resourcesApp = mock[ResourcesApp]
  val healthController = mock[HealthController]
  val listingController = mock[ListingController]
}
