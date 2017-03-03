/*
 * Part of NDLA utlisting_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.utlistingapi.integration

trait DataSource {
  val dataSource: javax.sql.DataSource
}
