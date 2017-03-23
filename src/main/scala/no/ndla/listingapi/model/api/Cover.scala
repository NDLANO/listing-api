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
                 @(ApiModelProperty@field)(description = "A cover photo for the cover") coverPhotoUrl: String,
                 @(ApiModelProperty@field)(description = "The title for this cover") title: String,
                 @(ApiModelProperty@field)(description = "The description for this cover") description: String,
                 @(ApiModelProperty@field)(description = "The link to the article") articleApiId: Long,
                 @(ApiModelProperty@field)(description = "The labels associated with this cover") labels: Seq[Label]
               )

@ApiModel(description = "Meta information for a cover")
case class Label(@(ApiModelProperty@field)(description = "The type of this label") `type`: Option[String],
                 @(ApiModelProperty@field)(description = "The label") labels: Seq[String]
                )

@ApiModel(description = "Information about search-results")
case class SearchResult(@(ApiModelProperty@field)(description = "The total number of covers matching this query") totalCount: Long,
                        @(ApiModelProperty@field)(description = "For which page results are shown from") page: Int,
                        @(ApiModelProperty@field)(description = "The number of results per page") pageSize: Int,
                        @(ApiModelProperty@field)(description = "The search results") results: Seq[Cover])
