/*
 * Part of NDLA listing_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.listingapi

import com.typesafe.scalalogging.LazyLogging
import no.ndla.listingapi.auth.{Client, Role}
import no.ndla.listingapi.controller.{
  HealthController,
  InternController,
  ListingController
}
import no.ndla.listingapi.integration._
import no.ndla.listingapi.repository.ListingRepository
import no.ndla.listingapi.service._
import no.ndla.listingapi.service.search.{
  IndexService,
  SearchConverterService,
  SearchIndexService,
  SearchService
}
import scalikejdbc.{ConnectionPool, DataSourceConnectionPool}

object ComponentRegistry
    extends DataSource
    with ListingRepository
    with ReadService
    with WriteService
    with CoverValidator
    with SearchService
    with Elastic4sClient
    with SearchIndexService
    with IndexService
    with SearchConverterService
    with ConverterService
    with ListingController
    with InternController
    with HealthController
    with Clock
    with Role
    with Client {
  def connectToDatabase(): Unit =
    ConnectionPool.singleton(new DataSourceConnectionPool(dataSource))

  implicit val swagger = new ListingSwagger

  lazy val dataSource = DataSource.getHikariDataSource
  connectToDatabase()

  lazy val listingController = new ListingController
  lazy val healthController = new HealthController
  lazy val resourcesApp = new ResourcesApp
  lazy val internController = new InternController

  lazy val listingRepository = new ListingRepository
  lazy val readService = new ReadService
  lazy val writeService = new WriteService
  lazy val converterService = new ConverterService
  lazy val coverValidator = new CoverValidator

  lazy val searchService = new SearchService
  lazy val e4sClient = Elastic4sClientFactory.getClient()
  lazy val searchIndexService = new SearchIndexService
  lazy val indexService = new IndexService
  lazy val searchConverterService = new SearchConverterService

  lazy val clock = new SystemClock
  lazy val authRole = new AuthRole
  lazy val authClient = new AuthClient
}
