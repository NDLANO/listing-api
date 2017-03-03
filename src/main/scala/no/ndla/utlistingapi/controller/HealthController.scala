/*
 * Part of NDLA utlisting_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.utlistingapi.controller

import org.scalatra.{Ok, ScalatraServlet}

trait HealthController {
  val healthController: HealthController

  class HealthController extends ScalatraServlet {

    get("/") {
      Ok()
    }
  }
}
