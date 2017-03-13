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

@ApiModel(description = "Meta information for a card")
case class Card(@(ApiModelProperty@field)(description = "The unique id of the card") id: Long,
                @(ApiModelProperty@field)(description = "A cover photo for the card") coverPhotoUrl: String,
                @(ApiModelProperty@field)(description = "The title for this card") title: String,
                @(ApiModelProperty@field)(description = "The description for this card") description: String,
                @(ApiModelProperty@field)(description = "The link to the article") articleApiId: Long,
                @(ApiModelProperty@field)(description = "The labels associated with this card") labels: Seq[Label]
               )

@ApiModel(description = "Meta information for a card")
case class Label(@(ApiModelProperty@field)(description = "The type of this label") `type`: Option[String],
                 @(ApiModelProperty@field)(description = "The label") labels: Seq[String]
                )
