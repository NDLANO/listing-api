/*
 * Part of NDLA listing_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.listingapi

import javax.sql.DataSource
import org.flywaydb.core.Flyway

object DBMigrator {
  def migrate(datasource: DataSource) = {
    val flyway = new Flyway()
    flyway.setDataSource(datasource)
    flyway.clean()
    println(flyway.info)
    flyway.migrate()
    flyway.setValidateOnMigrate(false)
    println(flyway.info.current().getDescription)
  }
}
