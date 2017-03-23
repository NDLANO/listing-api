package no.ndla.listingapi.service

import no.ndla.listingapi.model.domain.{LanguageLabels, getByLanguage}
import no.ndla.listingapi.model.{api, domain}

import scala.util.{Failure, Success}

trait ConverterService {
  val converterService: ConverterService

  class ConverterService {
    def toApiCover(cover: domain.Cover, language: String): Option[api.Cover] = {
      val title = getByLanguage[String, domain.Title](cover.title, language)
      val description = getByLanguage[String, domain.Description](cover.description, language)

      if (title.isEmpty || description.isEmpty) {
        return None
      }

      cover.getSupportedLanguages match {
        case Failure(e) => None
        case Success(langs) =>
          Some(api.Cover(
            cover.id.get,
            cover.coverPhotoUrl,
            title.get,
            description.get,
            cover.articleApiId,
            getByLanguage[Seq[domain.Label], LanguageLabels](cover.labels, language).getOrElse(Seq.empty).map(toApiLabel),
            langs
          ))
      }
    }


    private def toApiLabel(label: domain.Label): api.Label = {
      api.Label(label.`type`, label.labels)
    }

  }
}