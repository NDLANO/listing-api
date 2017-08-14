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

@ApiModel(description = "Title of the cover")
case class CoverTitle(@(ApiModelProperty@field)(description = "The freetext title of the cover") title: String,
                      @(ApiModelProperty@field)(description = "ISO 639-1 code that represents the language used in title") language: String)
