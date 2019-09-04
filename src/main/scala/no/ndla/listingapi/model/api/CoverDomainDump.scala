package no.ndla.listingapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}
import scala.annotation.meta.field
import no.ndla.listingapi.model.domain

@ApiModel(description = "Information about covers")
case class CoverDomainDump(
    @(ApiModelProperty @field)(description = "The total number of covers in the database") totalCount: Long,
    @(ApiModelProperty @field)(description = "The search results") results: Seq[domain.Cover])
