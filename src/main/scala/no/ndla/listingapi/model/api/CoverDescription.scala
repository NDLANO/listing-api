/*
 * Part of NDLA listing_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.listingapi.model.api

import org.scalatra.swagger.annotations._
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Meta information for a cover")
case class CoverDescription(
    @(ApiModelProperty @field)(description = "The description for this cover") description: String,
    @(ApiModelProperty @field)(description = "The language for this description") language: String,
)
