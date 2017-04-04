/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.listingapi.controller

import javax.servlet.http.HttpServletRequest

import com.typesafe.scalalogging.LazyLogging
import no.ndla.listingapi.ListingApiProperties.{CorrelationIdHeader, CorrelationIdKey}
import no.ndla.listingapi.model.api.{Error, NotFoundException, OptimisticLockException, ValidationError, ValidationException, ValidationMessage}
import no.ndla.network.{ApplicationUrl, CorrelationID}
import org.apache.logging.log4j.ThreadContext
import org.elasticsearch.index.IndexNotFoundException
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json.NativeJsonSupport
import org.scalatra._

abstract class NdlaController extends ScalatraServlet with NativeJsonSupport with LazyLogging {
  protected implicit override val jsonFormats: Formats = DefaultFormats

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
    case v: ValidationException => BadRequest(body=ValidationError(messages=v.errors))
    case n: NotFoundException => NotFound(body=Error(Error.NOT_FOUND, n.getMessage))
    case e: IndexNotFoundException => InternalServerError(body=Error.IndexMissingError)
    case o: OptimisticLockException => Conflict(body=Error(Error.RESOURCE_OUTDATED, o.getMessage))
    case t: Throwable => {
      logger.error(Error.GenericError.toString, t)
      InternalServerError(body=Error.GenericError)
    }
  }

  def paramOrDefault(paramName: String, default: String)(implicit request: HttpServletRequest): String =
    params.get(paramName).map(_.trim).filterNot(_.isEmpty()).getOrElse(default)

  def longOrDefault(paramName: String, default: Long): Long =
    paramOrDefault(paramName, default.toString).toLong

  def paramAsListOfString(paramName: String)(implicit request: HttpServletRequest): List[String] = {
    params.get(paramName) match {
      case None => List.empty
      case Some(param) => param.split(",").toList.map(_.trim)
    }
  }

  def paramAsListOfLong(paramName: String)(implicit request: HttpServletRequest): List[Long] = {
    val strings = paramAsListOfString(paramName)
    strings.headOption match {
      case None => List.empty
      case Some(_) =>
        if (!strings.forall(entry => entry.forall(_.isDigit))) {
          throw new ValidationException(errors=Seq(ValidationMessage(paramName, s"Invalid value for $paramName. Only (list of) digits are allowed.")))
        }
        strings.map(_.toLong)
    }
  }

  def long(paramName: String)(implicit request: HttpServletRequest): Long = {
    val paramValue = params(paramName)
    paramValue.forall(_.isDigit) match {
      case true => paramValue.toLong
      case false => throw new ValidationException(errors=Seq(ValidationMessage(paramName, s"Invalid value for $paramName. Only digits are allowed.")))
    }
  }

}

