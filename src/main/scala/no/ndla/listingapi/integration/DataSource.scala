/*
 * Part of NDLA listing_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.listingapi.integration

trait DataSource {
  val dataSource: javax.sql.DataSource
}
