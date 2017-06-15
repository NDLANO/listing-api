package no.ndla.listingapi.service

import no.ndla.listingapi.auth.User
import no.ndla.listingapi.model.api.{CoverAlreadyExistsException, NotFoundException}
import no.ndla.listingapi.model.domain._
import no.ndla.listingapi.model.{api, domain}
import no.ndla.listingapi.repository.ListingRepository
import no.ndla.listingapi.service.search.IndexService

import scala.util.{Failure, Success, Try}

trait WriteService {
  this: ConverterService with ListingRepository with ReadService with CoverValidator with IndexService with Clock with User =>
  val writeService: WriteService

  class WriteService {
    def newCover(cover: api.NewCover): Try[api.Cover] = {
      cover.oldNodeId.flatMap(listingRepository.getCoverWithOldNodeId) match {
        case Some(existingCover) => throw new CoverAlreadyExistsException(id=existingCover.id.get)
        case _ =>
      }

      val validCover = coverValidator.validate(converterService.toDomainCover(cover))
        .flatMap(domainCover => Try(listingRepository.insertCover(domainCover)))
        .flatMap(indexService.indexDocument)
        .flatMap(insertedCover => converterService.toApiCover(insertedCover, cover.language))

println(s"#1 .... before renew")
println(s"#1a .... before renew $readService")
println(s"#1b .... before renew ${readService.getAllLabelsMap}")
println(s"#1c .... before renew ${readService.allLabelsMap()}")
      readService.getAllLabelsMap.renewCache
      validCover
    }

    def updateCover(coverId: Long, cover: api.UpdateCover): Try[api.Cover] = {
      val updateCover = listingRepository.getCover(coverId) match {
        case None => Failure(new NotFoundException(s"No cover with id $coverId found"))
        case Some(existing) => Success(mergeCovers(existing, cover))
      }

      val updatedCover = updateCover.flatMap(coverValidator.validate)
        .flatMap(listingRepository.updateCover)
        .flatMap(indexService.indexDocument)
        .flatMap(updatedCover => converterService.toApiCover(updatedCover, cover.language))
println(s"#1a")
      val service = readService
println(s"#1b")
      val map = service.getAllLabelsMap
println(s"#1c")
      map.renewCache
      updatedCover
    }

    private[service] def mergeCovers(existing: domain.Cover, toMerge: api.UpdateCover): domain.Cover = {
      val id = authUser.id()
      val now = clock.now()

      existing.copy(
        articleApiId = toMerge.articleApiId.getOrElse(existing.articleApiId),
        revision = Some(toMerge.revision),
        coverPhotoUrl = toMerge.coverPhotoUrl.getOrElse(existing.coverPhotoUrl),
        title = mergeLanguageField[String, Title](existing.title, domain.Title(toMerge.title, Option(toMerge.language))),
        description = mergeLanguageField[String, Description](existing.description, domain.Description(toMerge.description, Option(toMerge.language))),
        labels = mergeLanguageField[Seq[Label], LanguageLabels](existing.labels, domain.LanguageLabels(toMerge.labels.map(converterService.toDomainLabel), Option(toMerge.language))),
        updatedBy = id,
        updated = now
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
