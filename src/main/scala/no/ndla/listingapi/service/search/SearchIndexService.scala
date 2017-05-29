/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.listingapi.service.search

import com.typesafe.scalalogging.LazyLogging
import no.ndla.listingapi.ListingApiProperties
import no.ndla.listingapi.model.domain.Cover
import no.ndla.listingapi.model.domain.search.ReindexResult
import no.ndla.listingapi.repository.ListingRepository

import scala.util.{Failure, Success, Try}

trait SearchIndexService {
  this: ListingRepository with IndexService =>
  val searchIndexService: SearchIndexService

  class SearchIndexService extends LazyLogging {

    def indexDocument(cover: Cover): Try[_] = {
      logger.info(s"indexDocument $cover")
      if (indexService.aliasTarget.isEmpty) {
        indexService.createIndex.map(newIndex => indexService.createAliasTarget(newIndex))
      }
      indexService.indexDocument(cover)
    }

    def indexDocuments: Try[ReindexResult] = {
      synchronized {
        val start = System.currentTimeMillis()
        indexService.createIndex.flatMap(indexName => {
          val operations = for {
            numIndexed <- indexDocuments(indexName)
            _ <- switchAliasTarget(indexName)
          } yield numIndexed

          operations match {
            case Failure(f) =>
              indexService.deleteIndex(indexName)
              Failure(f)
            case Success(totalIndexed) =>
              Success(ReindexResult(totalIndexed, System.currentTimeMillis() - start))
          }
        })
      }
    }

    private def switchAliasTarget(newIndex: String): Try[_] = {
      indexService.aliasTarget match {
        case Some(target) =>
          indexService.updateAliasTarget(target, newIndex).map(_ =>
            indexService.deleteIndex(target))
        case None => indexService.createAliasTarget(newIndex)
      }
    }

    private def indexDocuments(indexName: String): Try[Int] = {
      var numIndexed = 0
      getRanges.map(ranges => {
        ranges.foreach(range => {
          val numberInBulk = indexService.indexDocuments(listingRepository.cardsWithIdBetween(range._1, range._2), indexName)
          numberInBulk match {
            case Success(num) => numIndexed += num
            case Failure(f) => return Failure(f)
          }
        })
        numIndexed
      })
    }

    def getRanges:Try[List[(Long,Long)]] = {
      Try{
        val (minId, maxId) = listingRepository.minMaxId
        Seq.range(minId, maxId).grouped(ListingApiProperties.IndexBulkSize).map(group => (group.head, group.last + 1)).toList
      }
    }
  }
}
