package no.ndla.listingapi.service

import no.ndla.listingapi.model.domain.{LanguageLabels, getByLanguage}
import no.ndla.listingapi.model.{api, domain}

trait ConverterService {
  val converterService: ConverterService

  class ConverterService {
    def toApiCover(s: domain.Cover, language: String): Option[api.Cover] = {
      val title = getByLanguage[String, domain.Title](s.title, language)
      val description = getByLanguage[String, domain.Description](s.description, language)

      if (title.isEmpty || description.isEmpty) {
        return None
      }

      Some(api.Cover(
        s.id.get,
        s.coverPhotoUrl,
        title.get,
        description.get,
        s.articleApiId,
        getByLanguage[Seq[domain.Label], LanguageLabels](s.labels, language).getOrElse(Seq.empty).map(toApiLabel)))
    }

    private def toApiLabel(label: domain.Label): api.Label = {
      api.Label(label.`type`, label.labels)
    }

  }
}