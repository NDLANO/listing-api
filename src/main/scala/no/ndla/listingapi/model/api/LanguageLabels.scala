package no.ndla.listingapi.model.api

import org.scalatra.swagger.annotations.ApiModel
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Meta information for a cover")
case class LanguageLabels(@(ApiModelProperty@field)(description = "The label") labels: Seq[Label],
                          @(ApiModelProperty@field)(description = "ISO 639-1 code that represents the language used in the labels") language: String)

