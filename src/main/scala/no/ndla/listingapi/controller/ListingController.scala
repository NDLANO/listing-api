/*
 * Part of NDLA listing_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.listingapi.controller

import no.ndla.listingapi.ListingApiProperties.{CorrelationIdHeader, CorrelationIdKey, DefaultLanguage, DefaultPageSize}
import no.ndla.listingapi.model.api.{Error, ValidationError, ValidationException}
import no.ndla.listingapi.model.domain
import no.ndla.listingapi.model.domain.search.Sort
import no.ndla.listingapi.model.domain.{Label, LanguageLabels}
import no.ndla.listingapi.repository.ListingRepository
import no.ndla.listingapi.service.ReadService
import no.ndla.listingapi.service.search.SearchService
import no.ndla.network.{ApplicationUrl, CorrelationID}
import org.apache.logging.log4j.ThreadContext
import org.scalatra._
import org.scalatra.swagger.{ResponseMessage, Swagger, SwaggerSupport}

trait ListingController {
  this: ReadService with SearchService with ListingRepository =>
  val listingController: ListingController

  class ListingController(implicit val swagger: Swagger) extends NdlaController with SwaggerSupport {
    // Swagger-stuff
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

    get("/", operation(filterCoverDoc)) {
      val filter = paramAsListOfString("filter")
      val language = paramOrDefault("language", DefaultLanguage)
      val sort = Sort.valueOf(paramOrDefault("sort", "")).getOrElse(Sort.ByIdAsc)
      val pageSize = longOrDefault("page-size", DefaultPageSize)
      val page = longOrDefault("page", 1)

      searchService.matchingQuery(filter, language, page.toInt, pageSize.toInt, sort)
    }

    get("/:coverid", operation(getCoverDoc)) {
      val coverId = long("coverid")
      val language = params.get("language").getOrElse(DefaultLanguage)

      readService.coverWithId(coverId, language) match {
        case Some(cover) => cover
        case None => NotFound(body = Error(Error.NOT_FOUND, s"No cover with id $coverId found"))
      }
    }

  }
}
