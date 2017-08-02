/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.listingapi.service.search

import com.google.gson.JsonObject
import com.typesafe.scalalogging.LazyLogging
import io.searchbox.core.{Count, Search, SearchResult => JestSearchResult}
import io.searchbox.params.Parameters
import no.ndla.listingapi.ListingApiProperties.{MaxPageSize, SearchIndex}
import no.ndla.listingapi.integration.ElasticClient
import no.ndla.listingapi.model.api
import no.ndla.listingapi.model.api.NdlaSearchException
import no.ndla.listingapi.model.domain._
import no.ndla.listingapi.model.domain.search.Sort
import no.ndla.listingapi.model.meta.Theme
import no.ndla.listingapi.service.Clock
import org.apache.lucene.search.join.ScoreMode
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.index.IndexNotFoundException
import org.elasticsearch.index.query.{BoolQueryBuilder, QueryBuilders}
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.elasticsearch.search.sort.{FieldSortBuilder, SortBuilders, SortOrder}
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

trait SearchService {
  this: ElasticClient with SearchIndexService with SearchConverterService with Clock =>
  val searchService: SearchService

  class SearchService extends LazyLogging {

    private val noCopyright = QueryBuilders.boolQuery().mustNot(QueryBuilders.termQuery("license", "copyrighted"))

    def getHits(response: JestSearchResult, language: String): Seq[api.Cover] = {
      var resultList = Seq[api.Cover]()
      response.getTotal match {
        case count: Integer if count > 0 => {
          val resultArray = response.getJsonObject.get("hits").asInstanceOf[JsonObject].get("hits").getAsJsonArray
          val iterator = resultArray.iterator()
          while (iterator.hasNext) {
            val card = hitAsCard(iterator.next.asInstanceOf[JsonObject].get("_source").asInstanceOf[JsonObject], language)
            if (card.isDefined) {
              resultList = resultList :+ card.get
            }
          }
          resultList
        }
        case _ => Seq.empty
      }
    }

    def hitAsCard(hit: JsonObject, language: String): Option[api.Cover] = {
      import scala.collection.JavaConverters._
      val labelsOpt = Option(hit.get("labels").getAsJsonObject.get(language)).map(lang => lang.getAsJsonArray.asScala.map(_.getAsJsonObject))

      labelsOpt.map(labels => {
          api.Cover(
            hit.get("id").getAsLong,
            hit.get("revision").getAsInt,
            hit.get("coverPhotoUrl").getAsString,
            hit.get("title").getAsJsonObject.get(language).getAsString,
            hit.get("description").getAsJsonObject.get(language).getAsString,
            hit.get("articleApiId").getAsLong,
            labels.map(x => api.Label(Option(x.get("type")).map(_.getAsString), x.get("labels").getAsJsonArray.asScala.toSeq.map(_.getAsString))).toSeq,
            hit.get("supportedLanguages").getAsJsonArray.asScala.toSeq.map(_.getAsString),
            hit.get("updatedBy").getAsString,
            clock.toDate(hit.get("update").getAsString),
            hit.get("theme").getAsString,
            Option(hit.get("oldNodeId").getAsLong)
          )
      })
    }

    def all(language: String, page: Int, pageSize: Int, sort: Sort.Value): api.SearchResult = {
      executeSearch(
        language,
        sort,
        page,
        pageSize,
        QueryBuilders.boolQuery())
    }

    def matchingQuery(query: Seq[String], language: String, page: Int, pageSize: Int, sort: Sort.Value): api.SearchResult = {
      val qs = query.map(q => {
        val termQ = QueryBuilders.matchPhraseQuery(s"labels.$language.labels", q)
        val titleSearch = QueryBuilders.nestedQuery(s"labels.$language", termQ, ScoreMode.Avg)
        QueryBuilders.nestedQuery("labels", titleSearch, ScoreMode.Avg)
      })

      val filter = qs.foldLeft(QueryBuilders.boolQuery())((query, term) => {
        query.must(term)
      })

      executeSearch(language, sort, page, pageSize, filter)
    }

    private def executeSearch(language: String, sort: Sort.Value, page: Int, pageSize: Int, queryBuilder: BoolQueryBuilder): api.SearchResult = {
      val searchQuery = new SearchSourceBuilder().query(queryBuilder).sort(getSortDefinition(sort, language))

      val (startAt, numResults) = getStartAtAndNumResults(page, pageSize)
      val request = new Search.Builder(searchQuery.toString)
        .addIndex(SearchIndex)
        .setParameter(Parameters.SIZE, numResults)
        .setParameter("from", startAt)

        jestClient.execute(request.build()) match {
          case Success(response) => api.SearchResult(response.getTotal.toLong, page, numResults, getHits(response, language))
          case Failure(f) => errorHandler(Failure(f))
      }
    }

    def getSortDefinition(sort: Sort.Value, language: String): FieldSortBuilder = {
      sort match {
        case (Sort.ByIdAsc) => SortBuilders.fieldSort("id").order(SortOrder.ASC).missing("_last")
        case (Sort.ByIdDesc) => SortBuilders.fieldSort("id").order(SortOrder.DESC).missing("_last")
      }
    }

    def countDocuments(): Int = {
      val ret = jestClient.execute(
        new Count.Builder().addIndex(SearchIndex).build()
      ).map(result => result.getCount.toInt)
      ret.getOrElse(0)
    }

    def getStartAtAndNumResults(page: Int, pageSize: Int): (Int, Int) = {
      val numResults = pageSize.min(MaxPageSize)
      val startAt = (page - 1).max(0) * numResults

      (startAt, numResults)
    }

    private def errorHandler[T](failure: Failure[T]) = {
      failure match {
        case Failure(e: NdlaSearchException) => {
          e.getResponse.getResponseCode match {
            case notFound: Int if notFound == 404 => {
              logger.error(s"Index $SearchIndex not found. Scheduling a reindex.")
              scheduleIndexDocuments()
              throw new IndexNotFoundException(s"Index $SearchIndex not found. Scheduling a reindex")
            }
            case _ => {
              logger.error(e.getResponse.getErrorMessage)
              throw new ElasticsearchException(s"Unable to execute search in $SearchIndex", e.getResponse.getErrorMessage)
            }
          }

        }
        case Failure(t: Throwable) => throw t
      }
    }

    private def scheduleIndexDocuments() = {
      val f = Future {
        searchIndexService.indexDocuments
      }

      f.failed.foreach(t => logger.warn("unable to create index: " + t.getMessage, t))
      f.foreach {
        case Success(reindexResult) => logger.info(s"Completed indexing of ${reindexResult.totalIndexed} documents in ${reindexResult.millisUsed} ms.")
        case Failure(ex) => logger.warn(ex.getMessage, ex)
      }
    }

  }
}
