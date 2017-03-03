/*
 * Part of NDLA utlisting_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.utlistingapi

import no.ndla.utlistingapi.controller.{HealthController, UtlistingController}
import no.ndla.utlistingapi.integration.DataSource
import org.postgresql.ds.PGPoolingDataSource

object ComponentRegistry
  extends DataSource
  with UtlistingController
  with HealthController
{
  implicit val swagger = new UtlistingSwagger

  lazy val dataSource = new PGPoolingDataSource()
  dataSource.setUser(UtlistingApiProperties.MetaUserName)
  dataSource.setPassword(UtlistingApiProperties.MetaPassword)
  dataSource.setDatabaseName(UtlistingApiProperties.MetaResource)
  dataSource.setServerName(UtlistingApiProperties.MetaServer)
  dataSource.setPortNumber(UtlistingApiProperties.MetaPort)
  dataSource.setInitialConnections(UtlistingApiProperties.MetaInitialConnections)
  dataSource.setMaxConnections(UtlistingApiProperties.MetaMaxConnections)
  dataSource.setCurrentSchema(UtlistingApiProperties.MetaSchema)

  lazy val utlistingController = new UtlistingController
  lazy val healthController = new HealthController
  lazy val resourcesApp = new ResourcesApp
}
