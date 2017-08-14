package no.ndla.listingapi.service

import com.typesafe.scalalogging.LazyLogging
import no.ndla.listingapi.auth.User
import no.ndla.listingapi.model.api.{CoverDescription, CoverTitle, NotFoundException}
import no.ndla.listingapi.model.domain.{Label, LanguageLabels, getByLanguage}
import no.ndla.listingapi.model.{api, domain}

import scala.util.{Failure, Success, Try}

trait ConverterService {
  this: Clock with User =>
  val converterService: ConverterService

  class ConverterService extends LazyLogging{
    def toApiCover(cover: domain.Cover, language: String): Try[api.Cover] = {
      logger.info(s"toApiCover domain lang: $language cover $cover")
      val title = getByLanguage[String, domain.Title](cover.title, language)
      logger.info(s"title: $title")
      val description = getByLanguage[String, domain.Description](cover.description, language)
            val labels = getByLanguage[Seq[Label], LanguageLabels](cover.labels, language).map(toApiLabel)

      logger.info(s"description: $description")

      if (title.isEmpty || description.isEmpty) {
        Failure(new NotFoundException)
      } else {
        cover.getAllCoverLanguages match {
          case Failure(e) => Failure(e)
          case Success(langs) =>
            val cover1 = api.Cover(
              cover.id.get,
              cover.revision.get,
              cover.coverPhotoUrl,
              CoverTitle(title, language),
              CoverDescription(description, language),
              cover.articleApiId,
              Seq(api.LanguageLabels(labels, language)), //Sikkert feil
              langs,
              cover.updatedBy,
              cover.updated,
              cover.theme,
              createOembedUrl(cover.oldNodeId)
            )
            logger.info(s"cover1: $cover1")
            Success(cover1)
        }
      }
    }


    private def toApiLabel(label: domain.Label): api.Label = api.Label(label.`type`, label.labels)

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
        authUser.id(),
        clock.now(),
        cover.theme
      )
    }

    def toDomainLabel(label: api.Label): domain.Label = domain.Label(label.`type`, label.labels)

  }

}
