package no.ndla.listingapi.model.api

import org.scalatra.swagger.annotations.ApiModel
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Meta information for a updated cover")
case class UpdateCover(@(ApiModelProperty@field)(description = "The language in this request") language: String,
                       @(ApiModelProperty@field)(description = "The revision number of this cover") revision: Int,
                       @(ApiModelProperty@field)(description = "A cover photo for the cover") coverPhotoUrl: Option[String],
                       @(ApiModelProperty@field)(description = "The link to the article") articleApiId: Option[Long],
                       @(ApiModelProperty@field)(description = "The title for this cover") title: String,
                       @(ApiModelProperty@field)(description = "The description for this cover") description: String,
                       @(ApiModelProperty@field)(description = "The labels associated with this cover") labels: Seq[Label]
                   )
