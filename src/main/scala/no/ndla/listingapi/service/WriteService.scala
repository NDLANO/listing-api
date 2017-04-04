package no.ndla.listingapi.service

import no.ndla.listingapi.model.api.NotFoundException
import no.ndla.listingapi.model.domain._
import no.ndla.listingapi.model.{api, domain}
import no.ndla.listingapi.repository.ListingRepository
import no.ndla.listingapi.service.search.IndexService

import scala.util.{Failure, Success, Try}

trait WriteService {
  this: ConverterService with ListingRepository with CoverValidator with IndexService =>
  val writeService: WriteService

  class WriteService {
    def newCover(cover: api.NewCover): Try[api.Cover] = {
      coverValidator.validate(converterService.toDomainCover(cover))
        .flatMap(domainCover => Try(listingRepository.newCover(domainCover)))
        .flatMap(indexService.indexDocument)
        .flatMap(insertedCover => converterService.toApiCover(insertedCover, cover.language))
    }

    def updateCover(coverId: Long, cover: api.UpdateCover): Try[api.Cover] = {
      val updateCover = listingRepository.getCover(coverId) match {
        case None => Failure(new NotFoundException(s"No cover with id $coverId found"))
        case Some(existing) => Success(mergeCovers(existing, cover))
      }

      updateCover.flatMap(coverValidator.validate)
        .flatMap(listingRepository.updateCover)
        .flatMap(indexService.indexDocument)
        .flatMap(updatedCover => converterService.toApiCover(updatedCover, cover.language))
    }

    private[service] def mergeCovers(existing: domain.Cover, toMerge: api.UpdateCover): domain.Cover = {
      existing.copy(
        articleApiId = toMerge.articleApiId.getOrElse(existing.articleApiId),
        coverPhotoUrl = toMerge.coverPhotoUrl.getOrElse(existing.coverPhotoUrl),
        title = mergeLanguageField[String, Title](existing.title, domain.Title(toMerge.title, Option(toMerge.language))),
        description = mergeLanguageField[String, Description](existing.description, domain.Description(toMerge.description, Option(toMerge.language))),
        labels = mergeLanguageField[Seq[Label], LanguageLabels](existing.labels, domain.LanguageLabels(toMerge.labels.map(converterService.toDomainLabel), Option(toMerge.language)))
      )
    }

    private def mergeLanguageField[T, Y <: LanguageField[T]](field: Seq[Y], toMerge: Y): Seq[Y] = {
      field.indexWhere(_.language == toMerge.language) match {
        case idx if idx >= 0 => field.patch(idx, Seq(toMerge), 1)
        case _ => field ++ Seq(toMerge)
      }
    }

  }
}
