/*
 * Part of NDLA listing_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.listingapi.controller

import no.ndla.network.{ApplicationUrl, CorrelationID}
import no.ndla.listingapi.ListingApiProperties.{CorrelationIdHeader, CorrelationIdKey}
import no.ndla.listingapi.model.api.Error
import com.typesafe.scalalogging.LazyLogging
import org.apache.logging.log4j.ThreadContext
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.{InternalServerError, ScalatraServlet}
import org.scalatra.json.NativeJsonSupport
import org.scalatra.swagger.{Swagger, SwaggerSupport}

trait ListingController {
  val listingController: ListingController

  class ListingController(implicit val swagger: Swagger) extends ScalatraServlet with SwaggerSupport with NativeJsonSupport with LazyLogging {
    // Swagger-stuff
    protected implicit override val jsonFormats: Formats = DefaultFormats
    protected val applicationDescription = "API for grouping content from ndla.no."

    val helloDoc =
      (apiOperation[String]("getHello")
        summary "Hello world"
        notes "A Hello-world controller"
      )

    before() {
      contentType = formats("json")
      CorrelationID.set(Option(request.getHeader(CorrelationIdHeader)))
      ThreadContext.put(CorrelationIdKey, CorrelationID.get.getOrElse(""))
      ApplicationUrl.set(request)
      logger.info("{} {}{}", request.getMethod, request.getRequestURI, Option(request.getQueryString).map(s => s"?$s").getOrElse(""))
    }

    after() {
      CorrelationID.clear()
      ThreadContext.remove(CorrelationIdKey)
      ApplicationUrl.clear
    }

    error {
      case t: Throwable => {
        logger.error(Error.GenericError.toString, t)
        InternalServerError(body=Error.GenericError)
      }
    }

    get("/", operation(helloDoc)) {
      "Hello, world!"
    }

  }
}