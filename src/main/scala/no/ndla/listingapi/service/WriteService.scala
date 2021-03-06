package no.ndla.listingapi.service

import no.ndla.listingapi.auth.Client
import no.ndla.listingapi.model.api.{CoverAlreadyExistsException, NotFoundException}
import no.ndla.listingapi.model.domain._
import no.ndla.listingapi.model.{api, domain}
import no.ndla.listingapi.repository.ListingRepository
import no.ndla.listingapi.service.search.IndexService

import scala.util.{Failure, Success, Try}

trait WriteService {
  this: ConverterService
    with ListingRepository
    with ReadService
    with CoverValidator
    with IndexService
    with Clock
    with Client =>
  val writeService: WriteService

  class WriteService {

    def newCover(cover: api.NewCover): Try[api.Cover] = {
      cover.oldNodeId.flatMap(listingRepository.getCoverWithOldNodeId) match {
        case Some(existingCover) =>
          throw new CoverAlreadyExistsException(id = existingCover.id.get)
        case _ =>
      }

      val validCover = coverValidator
        .validate(converterService.toDomainCover(cover))
        .flatMap(domainCover => Try(listingRepository.insertCover(domainCover)))
        .flatMap(indexService.indexDocument)
        .map(insertedCover => converterService.toApiCover(insertedCover, cover.language))

      readService.getAllLabelsMap.renewCache
      validCover
    }

    def updateCover(coverId: Long, cover: api.UpdateCover): Try[api.Cover] = {
      val updateCover = listingRepository.getCover(coverId) match {
        case None =>
          Failure(new NotFoundException(s"No cover with id $coverId found"))
        case Some(existing) => Success(mergeCovers(existing, cover))
      }

      val updatedCover = updateCover
        .flatMap(coverValidator.validate)
        .flatMap(listingRepository.updateCover)
        .flatMap(indexService.indexDocument)
        .map(updatedCover => converterService.toApiCover(updatedCover, cover.language))

      readService.getAllLabelsMap.renewCache
      updatedCover
    }

    private[service] def mergeCovers(existing: domain.Cover, toMerge: api.UpdateCover): domain.Cover = {
      val id = authClient.client_id()
      val now = clock.now()

      existing.copy(
        articleApiId = toMerge.articleApiId.getOrElse(existing.articleApiId),
        revision = Some(toMerge.revision),
        coverPhotoUrl = toMerge.coverPhotoUrl.getOrElse(existing.coverPhotoUrl),
        title = mergeLanguageField[String, Title](existing.title, domain.Title(toMerge.title, toMerge.language)),
        description =
          mergeLanguageField[String, Description](existing.description,
                                                  domain.Description(toMerge.description, toMerge.language)),
        labels = mergeLanguageField[Seq[Label], LanguageLabels](
          existing.labels,
          domain.LanguageLabels(toMerge.labels.map(converterService.toDomainLabel), toMerge.language)),
        updatedBy = id,
        updated = now,
        theme = toMerge.theme
      )
    }

    private def mergeLanguageField[T, Y <: LanguageField[T]](field: Seq[Y], toMerge: Y): Seq[Y] = {
      field.indexWhere(_.language == toMerge.language) match {
        case idx if idx >= 0 => field.patch(idx, Seq(toMerge), 1)
        case _               => field ++ Seq(toMerge)
      }
    }

  }

}
