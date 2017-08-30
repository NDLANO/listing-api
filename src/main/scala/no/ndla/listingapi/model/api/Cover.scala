/*
 * Part of NDLA listing_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.listingapi.model.api

import java.util.Date

import org.scalatra.swagger.annotations._
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Meta information for a cover")
case class Cover(
  @(ApiModelProperty@field)(description = "The unique id of the cover") id: Long,
  @(ApiModelProperty@field)(description = "The revision number of this cover") revision: Int,
  @(ApiModelProperty@field)(description = "A cover photo for the cover") coverPhotoUrl: String,
  @(ApiModelProperty@field)(description = "The title for this cover") title: CoverTitle,
  @(ApiModelProperty@field)(description = "The description for this cover") description: CoverDescription,
  @(ApiModelProperty@field)(description = "The id to the article") articleApiId: Long,
  @(ApiModelProperty@field)(description = "The labels associated with this cover") labels: CoverLabels,
  @(ApiModelProperty@field)(description = "The languages this cover supports") supportedLanguages: Set[String],
  @(ApiModelProperty@field)(description = "The user id that last updated the cover") updatedBy: String,
  @(ApiModelProperty@field)(description = "When the cover was last updated") updated: Date,
  @(ApiModelProperty@field)(description = "The meta theme associated with this cover") theme: String,
  @(ApiModelProperty@field)(description = "The oembed url of the article") oembedUrl: Option[String],
)
