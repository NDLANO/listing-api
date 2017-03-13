/*
 * Part of NDLA listing_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.listingapi

import no.ndla.listingapi.controller.{HealthController, ListingController}
import no.ndla.listingapi.integration.DataSource
import no.ndla.listingapi.repository.ListingRepository
import no.ndla.listingapi.service.{ConverterService, ReadService}
import org.postgresql.ds.PGPoolingDataSource

object ComponentRegistry
  extends DataSource
  with ListingRepository
  with ReadService
  with ConverterService
  with ListingController
  with HealthController
{
  implicit val swagger = new ListingSwagger

  lazy val dataSource = new PGPoolingDataSource()
  dataSource.setUser(ListingApiProperties.MetaUserName)
  dataSource.setPassword(ListingApiProperties.MetaPassword)
  dataSource.setDatabaseName(ListingApiProperties.MetaResource)
  dataSource.setServerName(ListingApiProperties.MetaServer)
  dataSource.setPortNumber(ListingApiProperties.MetaPort)
  dataSource.setInitialConnections(ListingApiProperties.MetaInitialConnections)
  dataSource.setMaxConnections(ListingApiProperties.MetaMaxConnections)
  dataSource.setCurrentSchema(ListingApiProperties.MetaSchema)

  lazy val listingController = new ListingController
  lazy val healthController = new HealthController
  lazy val resourcesApp = new ResourcesApp
  lazy val listingRepository = new ListingRepository
  lazy val readService = new ReadService
  lazy val converterService = new ConverterService
}
