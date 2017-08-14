/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.listingapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Description of the cover")
case class CoverDescription(@(ApiModelProperty@field)(description = "The freetext description of the cover") description: String,
                      @(ApiModelProperty@field)(description = "ISO 639-1 code that represents the language used in title") language: String)
