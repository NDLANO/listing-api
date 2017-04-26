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

    def toDate(dateAsString :String): Date = {
      var formatter :DateTimeFormatter = DateTimeFormat.forPattern("YYYY-MM-DDThh:mm:ssZ");
      val changedIt = formatter.parseDateTime(dateAsString).toDate
      println(s"string [$dateAsString] as date [$changedIt]")
      return changedIt
    }
  }

}
