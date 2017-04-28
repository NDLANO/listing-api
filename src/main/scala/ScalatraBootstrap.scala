/*
 * Part of NDLA listing_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

import javax.servlet.ServletContext

import no.ndla.listingapi.ComponentRegistry
import org.scalatra.LifeCycle

class ScalatraBootstrap extends LifeCycle {

  override def init(context: ServletContext) {
    context.mount(ComponentRegistry.listingController, "/listing-api/v1/listing", "listing")
    context.mount(ComponentRegistry.healthController, "/health")
    context.mount(ComponentRegistry.internController, "/intern")
    context.mount(ComponentRegistry.resourcesApp, "/listing-api/api-docs")
  }

}
