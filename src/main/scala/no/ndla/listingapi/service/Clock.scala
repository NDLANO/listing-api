/*
 * Part of NDLA listing_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.listingapi.service

import java.util.Date

import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}

trait Clock {
  val clock: SystemClock

  class SystemClock {

    def now(): Date = {
      new Date()
    }

    def toDate(dateAsString: String): Date = {
      val formatter: DateTimeFormatter = DateTimeFormat.forPattern("YYYY-MM-DD'T'HH:mm:ssZ")
      val changedIt = formatter.parseDateTime(dateAsString).toDate

      return changedIt
    }
  }

}
