/*
 * Part of NDLA listing_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.listingapi.controller

import com.typesafe.scalalogging.LazyLogging
import no.ndla.listingapi.ListingApiProperties.{
  DefaultLanguage,
  DefaultPageSize,
  RoleWithWriteAccess
}
import no.ndla.listingapi.auth.Role
import no.ndla.listingapi.auth.Client
import no.ndla.listingapi.model.api.{
  Error,
  NewCover,
  ThemeResult,
  UpdateCover,
  ValidationError
}
import no.ndla.listingapi.model.domain.search.Sort
import no.ndla.listingapi.model.meta.Theme
import no.ndla.listingapi.repository.ListingRepository
import no.ndla.listingapi.service._
import no.ndla.listingapi.service.search.SearchService
import org.json4s.{DefaultFormats, Formats}
import org.scalatra._
import org.scalatra.swagger.{ResponseMessage, Swagger, SwaggerSupport}

trait ListingController {
  this: ReadService
    with SearchService
    with ListingRepository
    with WriteService
    with Role
    with Client =>
  val listingController: ListingController

  class ListingController(implicit val swagger: Swagger)
      extends NdlaController
      with SwaggerSupport
      with LazyLogging {
    protected implicit override val jsonFormats: Formats = DefaultFormats
    val response400 =
      ResponseMessage(400, "Validation error", Some("ValidationError"))

    registerModel[Error]
    registerModel[ValidationError]
    val response403 = ResponseMessage(403, "Access Denied", Some("Error"))
    val response404 = ResponseMessage(404, "Not found", Some("Error"))
    val response500 = ResponseMessage(500, "Unknown error", Some("Error"))
    val filterCoverDoc =
      (apiOperation[String]("filterCovers")
        summary "Returns covers matching a filter"
        notes "Returns a list of covers"
        parameters (
          queryParam[Option[String]]("filter")
            .description("A comma separated string containing labels"),
          queryParam[Option[String]]("language").description(
            s"Only return results on the given language. Default is $DefaultLanguage"),
          queryParam[Option[String]]("sort").description(
            s"Sort results. Valid options are ${Sort.values.mkString(", ")}"),
          queryParam[Option[Int]]("page-size")
            .description("Return this many results per page"),
          queryParam[Option[Int]]("page")
            .description("Return results for this page")
      )
        authorizations "oauth2"
        responseMessages (response500))
    val getCoverDoc =
      (apiOperation[String]("getCover")
        summary "Returns cover meta data"
        notes "Returns a cover"
        parameters (
          pathParam[Long]("coverid")
            .description("Id of the cover that is to be returned"),
          queryParam[Option[String]]("language").description(
            s"Return the cover on this language. Default is $DefaultLanguage")
      )
        authorizations "oauth2"
        responseMessages (response400, response404, response500))
    val newCoverDoc =
      (apiOperation[NewCover]("newCover")
        summary "Create a new cover"
        notes "Create a new cover. Returns the a json-document with then resulting cover"
        authorizations "oauth2"
        responseMessages (response400, response403, response404, response500))
    val updateCoverDoc =
      (apiOperation[UpdateCover]("updateCover")
        summary "Update a cover"
        notes "Update a cover with a new translation or update an existing translation"
        parameters (
          pathParam[Long]("coverid").description("ID of the cover to update")
        )
        authorizations "oauth2"
        responseMessages (response400, response403, response404, response500))
    val getLabelsDoc =
      (apiOperation[String]("getLabels")
        summary "Returns a map of all uniqe labels"
        notes s"Returns a map of all labels given the current language. Default is $DefaultLanguage"
        parameter (
          queryParam[Option[String]]("language").description(
            s"Return the labels on this language. Default is $DefaultLanguage")
        )
        authorizations "oauth2"
        responseMessages (response400, response403, response404, response500))
    val getThemeDoc =
      (apiOperation[ThemeResult]("getTheme")
        summary "Returns a sequence of covers with a named theme. Themes are predefined and should be known to the caller. "
        notes s"Returns  a sequence of covers with a named theme given the current language. Default is $DefaultLanguage."
        parameter (
          queryParam[Option[String]]("language").description(
            s"Return the covers of the theme in this language. Default is $DefaultLanguage")
        )
        authorizations "oauth2"
        responseMessages (response400, response403, response404, response500))

    protected val applicationDescription =
      "API for grouping content from ndla.no."

    post("/", operation(newCoverDoc)) {
      authClient.assertHasClientId()
      authRole.assertHasRole(RoleWithWriteAccess)
      writeService.newCover(extract[NewCover](request.body))
    }

    get("/", operation(filterCoverDoc)) {
      val filter = paramAsListOfString("filter")
      val language = paramOrDefault("language", DefaultLanguage)
      val sort =
        Sort.valueOf(paramOrDefault("sort", "")).getOrElse(Sort.ByIdAsc)
      val pageSize = longOrDefault("page-size", DefaultPageSize)
      val page = longOrDefault("page", 1)

      searchService.matchingQuery(filter,
                                  language,
                                  page.toInt,
                                  pageSize.toInt,
                                  sort)
    }

    get("/theme/:theme", operation(getThemeDoc)) {
      val theme = params("theme")
      val language = paramOrDefault("language", DefaultLanguage)
      theme match {
        case theme if Theme.allowedThemes.contains(theme) =>
          readService.getTheme(theme, language)
        case _ =>
          BadRequest(
            body = Error(Error.VALIDATION,
                         s"No theme with name '$theme' is configured."))
      }
    }

    put("/:coverid", operation(updateCoverDoc)) {
      authClient.assertHasClientId()
      authRole.assertHasRole(RoleWithWriteAccess)
      writeService.updateCover(long("coverid"),
                               extract[UpdateCover](request.body))
    }

    get("/:coverid", operation(getCoverDoc)) {
      val coverId = long("coverid")
      val language = paramOrDefault("language", DefaultLanguage)

      readService.coverWithId(coverId, language) match {
        case Some(cover) => cover
        case None =>
          NotFound(
            body = Error(Error.NOT_FOUND, s"No cover with id $coverId found."))
      }
    }

    get("/labels/", operation(getLabelsDoc)) {
      val language = paramOrDefault("language", DefaultLanguage)
      getLabels(language, "all")
    }

    get("/labels/:type", operation(getLabelsDoc)) {
      val language = paramOrDefault("language", DefaultLanguage)
      val labelType: String = paramOrDefault("type", "all")
      getLabels(language, labelType)
    }

    private def getLabels(language: String, labelType: String) = {
      val allLabels = readService.allLabelsMap()
      val theLangueLabels = allLabels.get(language)

      theLangueLabels match {
        case Some(uniqeLabels) => {
          if (labelType.equalsIgnoreCase("all")) {
            Ok(uniqeLabels.labelsByType)
          } else {
            Ok(uniqeLabels.labelsByType.get(labelType))
          }
        }
        case None => Ok(Nil)
      }
    }

  }

}
