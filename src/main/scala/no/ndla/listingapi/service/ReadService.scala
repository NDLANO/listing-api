package no.ndla.listingapi.service

import com.typesafe.scalalogging.LazyLogging
import no.ndla.listingapi.caching.MemoizeAutoRenew
import no.ndla.listingapi.model.api
import no.ndla.listingapi.repository.ListingRepository

trait ReadService {
  this: ListingRepository with ConverterService =>
  val readService: ReadService

  class ReadService extends LazyLogging {
    def coverWithId(id: Long, language: String): Option[api.Cover] = {
      listingRepository.getCover(id).flatMap(c => converterService.toApiCover(c, language).toOption)
    }

    def uniqeLabelsMap(language: String): Map[String, Set[String]] = {
      logger.info(s"readService uniq labels map for lang $language")
      getUniqeLabelsMap(language).apply()
    }

    def getUniqeLabelsMap(lang: String) = MemoizeAutoRenew(() => {
      logger.info(s"getUniqeLabelsMap $lang from cache?")
      listingRepository.allUniqeLabelsByType(lang)
    })
  }
}
