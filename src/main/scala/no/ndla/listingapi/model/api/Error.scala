/*
 * Part of NDLA listing_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.listingapi.model.api

import no.ndla.listingapi.ListingApiProperties
import java.util.Date

import com.sksamuel.elastic4s.http.RequestFailure
import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Information about an error")
case class Error(
    @(ApiModelProperty @field)(description = "Code stating the type of error") code: String = Error.GENERIC,
    @(ApiModelProperty @field)(description = "Description of the error") description: String = Error.GENERIC_DESCRIPTION,
    @(ApiModelProperty @field)(description = "An optional id referring to the cover") id: Option[Long] = None,
    @(ApiModelProperty @field)(description = "When the error occured") occuredAt: Date = new Date())

@ApiModel(description = "Information about validation errors")
case class ValidationError(
    @(ApiModelProperty @field)(description = "Code stating the type of error") code: String = Error.VALIDATION,
    @(ApiModelProperty @field)(description = "Description of the error") description: String =
      Error.VALIDATION_DESCRIPTION,
    @(ApiModelProperty @field)(description = "List of validation messages") messages: Seq[ValidationMessage],
    @(ApiModelProperty @field)(description = "When the error occured") occuredAt: Date = new Date())

object Error {
  val GENERIC = "GENERIC"
  val NOT_FOUND = "NOT_FOUND"
  val VALIDATION = "VALIDATION"
  val INDEX_MISSING = "INDEX_MISSING"
  val RESOURCE_OUTDATED = "RESOURCE_OUTDATED"
  val ACCESS_DENIED = "ACCESS DENIED"
  val ALREADY_EXISTS = "ALREADY_EXISTS"

  val GENERIC_DESCRIPTION =
    s"Ooops. Something we didn't anticipate occured. We have logged the error, and will look into it. But feel free to contact ${ListingApiProperties.ContactEmail} if the error persists."
  val VALIDATION_DESCRIPTION = "Validation Error"

  val INDEX_MISSING_DESCRIPTION =
    s"Ooops. Our search index is not available at the moment, but we are trying to recreate it. Please try again in a few minutes. Feel free to contact ${ListingApiProperties.ContactEmail} if the error persists."

  val RESOURCE_OUTDATED_DESCRIPTION =
    "The resource is outdated. Please try fetching before submitting again."
  val WINDOW_TOO_LARGE = "RESULT WINDOW TOO LARGE"
  val DATABASE_UNAVAILABLE = "DATABASE_UNAVAILABLE"

  val GenericError = Error(GENERIC, GENERIC_DESCRIPTION)
  val IndexMissingError = Error(INDEX_MISSING, INDEX_MISSING_DESCRIPTION)

  val WindowTooLargeError = Error(
    WINDOW_TOO_LARGE,
    s"The result window is too large. Fetching pages above ${ListingApiProperties.ElasticSearchIndexMaxResultWindow} results are unsupported."
  )
  val DatabaseUnavailableError = Error(DATABASE_UNAVAILABLE, s"Database seems to be unavailable, retrying connection.")
}

class NotFoundException(message: String = "The cover was not found") extends RuntimeException(message)

class CoverAlreadyExistsException(message: String = "This cover already exists", val id: Long)
    extends RuntimeException(message)

class ValidationException(message: String = "Validation Error", val errors: Seq[ValidationMessage])
    extends RuntimeException(message)
class AccessDeniedException(message: String) extends RuntimeException(message)
class OptimisticLockException(message: String = Error.RESOURCE_OUTDATED_DESCRIPTION) extends RuntimeException(message)
case class NdlaSearchException(rf: RequestFailure)
    extends RuntimeException(
      s"""
     |index: ${rf.error.index.getOrElse("Error did not contain index")}
     |reason: ${rf.error.reason}
     |body: ${rf.body}
     |shard: ${rf.error.shard.getOrElse("Error did not contain shard")}
     |type: ${rf.error.`type`}
   """.stripMargin
    )

class ResultWindowTooLargeException(message: String = Error.WindowTooLargeError.description)
    extends RuntimeException(message)
