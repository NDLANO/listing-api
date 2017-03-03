/*
 * Part of NDLA utlisting_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

import javax.servlet.ServletContext

import no.ndla.utlistingapi.ComponentRegistry
import org.scalatra.LifeCycle

class ScalatraBootstrap extends LifeCycle {

  override def init(context: ServletContext) {
    context.mount(ComponentRegistry.utlistingController, "/utlisting-api/v1/utlisting", "utlisting")
    context.mount(ComponentRegistry.healthController, "/health")
    context.mount(ComponentRegistry.resourcesApp, "/api-docs")
  }

}
