package no.ndla.listingapi.service

import com.typesafe.scalalogging.LazyLogging
import no.ndla.listingapi.caching.MemoizeAutoRenew
import no.ndla.listingapi.model.api
import no.ndla.listingapi.model.api.ThemeResult
import no.ndla.listingapi.model.domain.{Lang, ThemeName, UniqeLabels}
import no.ndla.listingapi.repository.ListingRepository

trait ReadService {
  this: ListingRepository with ConverterService =>
  val readService: ReadService

  class ReadService extends LazyLogging {
    val getAllLabelsMap = MemoizeAutoRenew(listingRepository.allLabelsMap _)

    def coverWithId(id: Long, language: String): Option[api.Cover] =
      listingRepository
        .getCover(id)
        .map(c => converterService.toApiCover(c, language))

    def allLabelsMap(): Map[Lang, UniqeLabels] = {
      getAllLabelsMap()
    }

    def getTheme(theme: ThemeName, language: String): api.ThemeResult = {
      val covers = listingRepository
        .getTheme(theme)
        .map(t => converterService.toApiCover(t, language))
      ThemeResult(covers.length, covers)
    }

  }

}
