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

    val filterCardsDoc =
      (apiOperation[String]("filterCards")
        summary "Returns cards matching a filter"
        notes "Returns a list of cards"
        parameters(
          queryParam[String]("filter").description("A comma separated string containin labels")
        )
        authorizations "oauth2"
        responseMessages(response500))

    val getCardDoc =
      (apiOperation[String]("getCard")
        summary "Returns card meta data"
        notes "Returns a card"
        parameters(
          pathParam[Long]("cardid").description("Id of the card that is to be returned")
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

    get("/:cardid", operation(getCardDoc)) {
      val cardId = long("cardid")

      readService.cardWithId(cardId) match {
        case Some(card) => card
        case None => NotFound(body = Error(Error.NOT_FOUND, s"No card with id $cardId found"))
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
