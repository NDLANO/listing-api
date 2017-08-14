package no.ndla.listingapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Meta information for a cover")
case class Label(@(ApiModelProperty@field)(description = "The type of this label") `type`: Option[String],
                 @(ApiModelProperty@field)(description = "The label") labels: Seq[String]
                )
