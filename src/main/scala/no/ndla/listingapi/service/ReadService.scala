package no.ndla.listingapi.service

import no.ndla.listingapi.model.api
import no.ndla.listingapi.repository.ListingRepository

trait ReadService {
  this: ListingRepository with ConverterService =>
  val readService: ReadService

  class ReadService {
    def coverWithId(id: Long, language: String): Option[api.Cover] = {
      listingRepository.getCover(id).flatMap(c => converterService.toApiCover(c, language))
    }
  }
}
