package no.ndla.listingapi.model.api

import no.ndla.listingapi.model.domain.ThemeName
import no.ndla.listingapi.model.meta.Theme
import org.scalatra.swagger.annotations.ApiModel
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Meta information for a new cover")
case class NewCover(
    @(ApiModelProperty @field)(description = "The language in this request") language: String,
    @(ApiModelProperty @field)(description = "A cover photo for the cover") coverPhotoUrl: String,
    @(ApiModelProperty @field)(description = "The title for this cover") title: String,
    @(ApiModelProperty @field)(description = "The description for this cover") description: String,
    @(ApiModelProperty @field)(description = "The link to the article") articleApiId: Long,
    @(ApiModelProperty @field)(description = "The id of the old article") oldNodeId: Option[Long],
    @(ApiModelProperty @field)(description = "The labels associated with this cover") labels: Seq[Label],
    @(ApiModelProperty @field)(description = "The meta theme associated with this cover") theme: String)
