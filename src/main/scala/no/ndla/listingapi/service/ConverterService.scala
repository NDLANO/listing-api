package no.ndla.listingapi.service

import no.ndla.listingapi.model.{api, domain}

trait ConverterService {
  val converterService: ConverterService

  class ConverterService {
    def toApiCard(s: domain.Card): api.Card = {
      api.Card(s.id.get, s.coverPhotoUrl, s.title, s.description, s.articleId, s.labels.map(toApiLabel))
    }

    private def toApiLabel(label: domain.Label): api.Label = {
      api.Label(label.`type`, label.labels)
    }

  }
}