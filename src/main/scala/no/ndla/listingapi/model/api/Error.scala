/*
 * Part of NDLA listing_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.listingapi.model.api

import java.util.Date

import no.ndla.listingapi.ListingApiProperties
import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Information about an error")
case class Error(@(ApiModelProperty@field)(description = "Code stating the type of error") code: String = Error.GENERIC,
                 @(ApiModelProperty@field)(description = "Description of the error") description: String = Error.GENERIC_DESCRIPTION,
                 @(ApiModelProperty@field)(description = "When the error occured") occuredAt: Date = new Date())

@ApiModel(description = "Information about validation errors")
case class ValidationError(@(ApiModelProperty@field)(description = "Code stating the type of error") code: String = Error.VALIDATION,
                           @(ApiModelProperty@field)(description = "Description of the error") description: String = Error.VALIDATION_DESCRIPTION,
                           @(ApiModelProperty@field)(description = "List of validation messages") messages: Seq[ValidationMessage],
                           @(ApiModelProperty@field)(description = "When the error occured") occuredAt: Date = new Date())

object Error {
  val GENERIC = "GENERIC"
  val NOT_FOUND = "NOT_FOUND"
  val VALIDATION = "VALIDATION"

  val GENERIC_DESCRIPTION = s"Ooops. Something we didn't anticipate occured. We have logged the error, and will look into it. But feel free to contact ${ListingApiProperties.ContactEmail} if the error persists."
  val VALIDATION_DESCRIPTION = "Validation Error"

  val GenericError = Error(GENERIC, GENERIC_DESCRIPTION)
}

class NotFoundException(message: String = "The cover was not found") extends RuntimeException(message)
