package no.ndla.listingapi.service

import com.typesafe.scalalogging.LazyLogging
import no.ndla.listingapi.caching.MemoizeAutoRenew
import no.ndla.listingapi.model.api
import no.ndla.listingapi.model.domain.{Lang, UniqeLabels}
import no.ndla.listingapi.repository.ListingRepository

trait ReadService {
  this: ListingRepository with ConverterService =>
  val readService: ReadService

  class ReadService extends LazyLogging {
    def coverWithId(id: Long, language: String): Option[api.Cover] = {
      listingRepository.getCover(id).flatMap(c => converterService.toApiCover(c, language).toOption)
    }

    def allLabelsMap(): Map[Lang, UniqeLabels] = {
      getAllLabelsMap().apply()
    }

    def getAllLabelsMap() = MemoizeAutoRenew(() => {
      listingRepository.allLabelsMap()
    })

  }
}
