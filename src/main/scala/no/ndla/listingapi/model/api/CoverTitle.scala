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
case class CoverTitle(@(ApiModelProperty @field)(
                        description = "The title for this cover") title: String,
                      @(ApiModelProperty @field)(description =
                        "The language for this title") language: String,
)
