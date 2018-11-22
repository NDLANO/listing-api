package no.ndla.listingapi.model.api

import org.scalatra.swagger.annotations.ApiModel
import org.scalatra.swagger.runtime.annotations.ApiModelProperty

import scala.annotation.meta.field

@ApiModel(description = "Information about theme-results") case class ThemeResult(
    @(ApiModelProperty @field)(description = "The total number of covers matching this theme") totalCount: Long,
    @(ApiModelProperty @field)(description = "The search results") results: Seq[Cover]
)
