package no.ndla.listingapi.service

import no.ndla.listingapi.model.api
import no.ndla.listingapi.repository.ListingRepository

trait ReadService {
  this: ListingRepository with ConverterService =>
  val readService: ReadService

  class ReadService {
    def cardWithId(id: Long): Option[api.Card] = {
      listingRepository.getCard(id).map(converterService.toApiCard)
    }
  }
}
