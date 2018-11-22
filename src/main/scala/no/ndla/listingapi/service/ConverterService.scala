package no.ndla.listingapi.service

import no.ndla.listingapi.auth.Client
import no.ndla.listingapi.model.api.NotFoundException
import no.ndla.listingapi.model.domain.search.Language.{DefaultLanguage, findByLanguageOrBestEffort}
import no.ndla.listingapi.model.domain.{LanguageLabels, getByLanguage}
import no.ndla.listingapi.model.{api, domain}

import scala.util.{Failure, Success, Try}

trait ConverterService {
  this: Clock with Client =>
  val converterService: ConverterService

  class ConverterService {

    def toApiCover(cover: domain.Cover, language: String): api.Cover = {
      val title = toApiCoverTitle(cover.title, language)
      val description = toApiCoverDescription(cover.description, language)
      val labels = toApiCoverLabels(cover.labels, language)

      api.Cover(
        cover.id.get,
        cover.revision.get,
        cover.coverPhotoUrl,
        title,
        description,
        cover.articleApiId,
        labels,
        cover.supportedLanguages,
        cover.updatedBy,
        cover.updated,
        cover.theme,
        createOembedUrl(cover.oldNodeId)
      )
    }

    def toApiCoverTitle(titles: Seq[domain.Title], language: String): api.CoverTitle = {
      findByLanguageOrBestEffort(titles, language)
        .map(t => api.CoverTitle(t.title, t.language))
        .getOrElse(api.CoverTitle("", DefaultLanguage))
    }

    def toApiCoverDescription(descriptions: Seq[domain.Description], language: String): api.CoverDescription = {
      findByLanguageOrBestEffort(descriptions, language)
        .map(t => api.CoverDescription(t.description, t.language))
        .getOrElse(api.CoverDescription("", DefaultLanguage))
    }

    private def toApiCoverLabels(labels: Seq[LanguageLabels], language: String): api.CoverLabels = {
      findByLanguageOrBestEffort(labels, language)
        .map(t => api.CoverLabels(t.labels.map(toApiCoverLabel), t.language))
        .getOrElse(api.CoverLabels(Seq.empty, DefaultLanguage))
    }

    def toApiCoverLabel(label: domain.Label): api.Label =
      api.Label(label.`type`, label.labels)

    def toDomainCover(cover: api.NewCover): domain.Cover = {
      domain.Cover(
        None,
        None,
        cover.oldNodeId,
        cover.coverPhotoUrl,
        Seq(domain.Title(cover.title, cover.language)),
        Seq(domain.Description(cover.description, cover.language)),
        Seq(LanguageLabels(cover.labels.map(toDomainLabel), cover.language)),
        cover.articleApiId,
        authClient.client_id(),
        clock.now(),
        cover.theme
      )
    }

    def toDomainLabel(label: api.Label): domain.Label =
      domain.Label(label.`type`, label.labels)

  }

}
