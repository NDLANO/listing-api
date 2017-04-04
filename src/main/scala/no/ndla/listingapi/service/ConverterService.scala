package no.ndla.listingapi.service

import no.ndla.listingapi.model.api.NotFoundException
import no.ndla.listingapi.model.domain.{LanguageLabels, getByLanguage}
import no.ndla.listingapi.model.{api, domain}

import scala.util.{Failure, Success, Try}

trait ConverterService {
  val converterService: ConverterService

  class ConverterService {
    def toApiCover(cover: domain.Cover, language: String): Try[api.Cover] = {
      val title = getByLanguage[String, domain.Title](cover.title, language)
      val description = getByLanguage[String, domain.Description](cover.description, language)

      if (title.isEmpty || description.isEmpty) {
        return Failure(new NotFoundException)
      }

      cover.getAllCoverLanguages match {
        case Failure(e) => Failure(e)
        case Success(langs) =>
          Success(api.Cover(
            cover.id.get,
            cover.revision.get,
            cover.coverPhotoUrl,
            title.get,
            description.get,
            cover.articleApiId,
            getByLanguage[Seq[domain.Label], LanguageLabels](cover.labels, language).getOrElse(Seq.empty).map(toApiLabel),
            langs
          ))
      }
    }

    private def toApiLabel(label: domain.Label): api.Label = api.Label(label.`type`, label.labels)

    def toDomainCover(cover: api.NewCover): domain.Cover = {
      domain.Cover(
        None,
        None,
        cover.coverPhotoUrl,
        Seq(domain.Title(cover.title, Option(cover.language))),
        Seq(domain.Description(cover.description, Option(cover.language))),
        Seq(LanguageLabels(cover.labels.map(toDomainLabel), Option(cover.language))),
        cover.articleApiId
      )
    }

    def toDomainLabel(label: api.Label): domain.Label = domain.Label(label.`type`, label.labels)

  }
}
