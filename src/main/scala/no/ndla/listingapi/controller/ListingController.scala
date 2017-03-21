/*
 * Part of NDLA listing_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.listingapi.controller

import javax.servlet.http.HttpServletRequest

import com.typesafe.scalalogging.LazyLogging
import no.ndla.listingapi.ListingApiProperties.{CorrelationIdHeader, CorrelationIdKey}
import no.ndla.listingapi.model.api.{Error, ValidationError, ValidationException}
import no.ndla.listingapi.service.ReadService
import no.ndla.network.{ApplicationUrl, CorrelationID}
import org.apache.logging.log4j.ThreadContext
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json.NativeJsonSupport
import org.scalatra.swagger.{ResponseMessage, Swagger, SwaggerSupport}
import org.scalatra._

trait ListingController {
  this: ReadService =>
  val listingController: ListingController

  class ListingController(implicit val swagger: Swagger) extends ScalatraServlet with SwaggerSupport with NativeJsonSupport with LazyLogging {
    // Swagger-stuff
    protected implicit override val jsonFormats: Formats = DefaultFormats
    protected val applicationDescription = "API for grouping content from ndla.no."

    registerModel[Error]

    val response404 = ResponseMessage(404, "Not found", Some("Error"))
    val response500 = ResponseMessage(500, "Unknown error", Some("Error"))

    val filterCoverDoc =
      (apiOperation[String]("filterCovers")
        summary "Returns covers matching a filter"
        notes "Returns a list of covers"
        parameters(
          queryParam[String]("filter").description("A comma separated string containing labels")
        )
        authorizations "oauth2"
        responseMessages(response500))

    val getCoverDoc =
      (apiOperation[String]("getCover")
        summary "Returns cover meta data"
        notes "Returns a cover"
        parameters(
          pathParam[Long]("coverid").description("Id of the cover that is to be returned")
        )
        authorizations "oauth2"
        responseMessages(response404, response500))

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
      case v: ValidationException => BadRequest(body=ValidationError(message=v.getMessage))
      case t: Throwable => {
        logger.error(Error.GenericError.toString, t)
        InternalServerError(body=Error.GenericError)
      }
    }

    get("/:coverid", operation(getCoverDoc)) {
      val coverId = long("coverid")

      readService.coverWithId(coverId) match {
        case Some(cover) => cover
        case None => NotFound(body = Error(Error.NOT_FOUND, s"No cover with id $coverId found"))
      }
    }

    def long(paramName: String)(implicit request: HttpServletRequest): Long = {
      val paramValue = params(paramName)
      paramValue.forall(_.isDigit) match {
        case true => paramValue.toLong
        case false => throw new ValidationException(s"Invalid value for $paramName. Only digits are allowed.")
      }
    }

  }
}
