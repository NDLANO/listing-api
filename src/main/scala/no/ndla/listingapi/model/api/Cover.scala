/*
 * Part of NDLA listing_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.listingapi.model.api

import org.scalatra.swagger.annotations._
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Meta information for a cover")
case class Cover(@(ApiModelProperty@field)(description = "The unique id of the cover") id: Long,
                 @(ApiModelProperty@field)(description = "The revision number of this cover") revision: Int,
                 @(ApiModelProperty@field)(description = "A cover photo for the cover") coverPhotoUrl: String,
                 @(ApiModelProperty@field)(description = "The title for this cover") title: String,
                 @(ApiModelProperty@field)(description = "The description for this cover") description: String,
                 @(ApiModelProperty@field)(description = "The link to the article") articleApiId: Long,
                 @(ApiModelProperty@field)(description = "The labels associated with this cover") labels: Seq[Label],
                 @(ApiModelProperty@field)(description = "The languages this cover supports") supportedLanguages: Seq[String]
               )
