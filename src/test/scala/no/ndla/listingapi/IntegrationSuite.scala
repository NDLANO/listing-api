/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.listingapi

import javax.sql.DataSource

import no.ndla.network.secrets.PropertyKeys
import org.postgresql.ds.PGPoolingDataSource

abstract class IntegrationSuite extends UnitSuite {

  setEnv(PropertyKeys.MetaUserNameKey, "postgres")
  setEnvIfAbsent(PropertyKeys.MetaPasswordKey, "hemmelig")
  setEnv(PropertyKeys.MetaResourceKey, "postgres")
  setEnv(PropertyKeys.MetaServerKey, "127.0.0.1")
  setEnv(PropertyKeys.MetaPortKey, "5432")
  setEnv(PropertyKeys.MetaSchemaKey, "listingapitest")
  setEnv("RUN_WITH_SIGNED_SEARCH_REQUESTS", "false")


  def getDataSource: DataSource = {
    val datasource = new PGPoolingDataSource()
    datasource.setUser(ListingApiProperties.MetaUserName)
    datasource.setPassword(ListingApiProperties.MetaPassword)
    datasource.setDatabaseName(ListingApiProperties.MetaResource)
    datasource.setServerName(ListingApiProperties.MetaServer)
    datasource.setPortNumber(ListingApiProperties.MetaPort)
    datasource.setInitialConnections(ListingApiProperties.MetaInitialConnections)
    datasource.setMaxConnections(ListingApiProperties.MetaMaxConnections)
    datasource.setCurrentSchema(ListingApiProperties.MetaSchema)
    datasource
  }
}
