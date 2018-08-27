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
      for {
        _ <- indexService.aliasTarget.map {
          case Some(index) => Success(index)
          case None =>
            indexService.createIndexWithGeneratedName.map(newIndex =>
              indexService.updateAliasTarget(None, newIndex))
        }
        imported <- indexService.indexDocument(cover)
      } yield imported
    }

    def indexDocuments: Try[ReindexResult] = {
      synchronized {
        val start = System.currentTimeMillis()
        indexService.createIndexWithGeneratedName.flatMap(indexName => {
          val operations = for {
            numIndexed <- indexDocuments(indexName)
            aliasTarget <- indexService.aliasTarget
            _ <- indexService.updateAliasTarget(aliasTarget, indexName)
            _ <- indexService.deleteIndexWithName(aliasTarget)
          } yield numIndexed

          operations match {
            case Failure(f) => {
              indexService.deleteIndexWithName(Some(indexName))
              Failure(f)
            }
            case Success(totalIndexed) => {
              Success(
                ReindexResult(totalIndexed, System.currentTimeMillis() - start))
            }
          }
        })
      }
    }

    private def indexDocuments(indexName: String): Try[Int] = {
      var numIndexed = 0
      getRanges.map(ranges => {
        ranges.foreach(range => {
          val numberInBulk = indexService.indexDocuments(
            listingRepository.cardsWithIdBetween(range._1, range._2),
            indexName)
          numberInBulk match {
            case Success(num) => numIndexed += num
            case Failure(f)   => return Failure(f)
          }
        })
        numIndexed
      })
    }

    def getRanges: Try[List[(Long, Long)]] = {
      Try {
        val (minId, maxId) = listingRepository.minMaxId
        Seq
          .range(minId, maxId)
          .grouped(ListingApiProperties.IndexBulkSize)
          .map(group => (group.head, group.last + 1))
          .toList
      }
    }
  }

}
