/*
 * Part of NDLA listing_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.listingapi.model.api

import org.scalatra.swagger.annotations.ApiModel
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field


@ApiModel(description = "All current subjects for all covers")
class Subjects(
  @(ApiModelProperty@field)(description = "The language in this request") language: String,
  @(ApiModelProperty@field)(description = "The subjects for all covers") subjects: Seq[String]
)
