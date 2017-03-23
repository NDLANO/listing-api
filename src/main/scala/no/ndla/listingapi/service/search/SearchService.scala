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
import no.ndla.listingapi.ListingApiProperties.{MaxPageSize, SearchDocument, SearchIndex}
import no.ndla.listingapi.integration.ElasticClient
import no.ndla.listingapi.model.api
import no.ndla.listingapi.model.domain._
import no.ndla.listingapi.model.domain.search.Sort
import org.apache.lucene.search.join.ScoreMode
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.index.IndexNotFoundException
import org.elasticsearch.index.query.{BoolQueryBuilder, NestedQueryBuilder, Operator, QueryBuilders}
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.elasticsearch.search.sort.{FieldSortBuilder, SortBuilders, SortOrder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

trait SearchService {
  this: ElasticClient with SearchIndexService with SearchConverterService =>
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
            resultList = resultList :+ hitAsCard(iterator.next.asInstanceOf[JsonObject].get("_source").asInstanceOf[JsonObject], language)
          }
          resultList
        }
        case _ => Seq.empty
      }
    }

    def hitAsCard(hit: JsonObject, language: String): api.Cover = {
      import scala.collection.JavaConverters._

//    [{"lang":"nb","value":[{"type":"kategori","labels":["personlig verktøy"]},{"type":"subject","labels":["betongfaget","murerfaget","tømrerfaget"]}]}]
      logger.info(s"Fetching card with language $language: ${hit.get("labels")}")
      logger.info(s"title: ${hit.get("title")}")

      val labels = hit.get("labels").getAsJsonObject.get(language).getAsJsonArray.asScala.map(_.getAsJsonObject)

      api.Cover(
        hit.get("id").getAsLong,
        hit.get("coverPhotoUrl").getAsString,
        hit.get("title").getAsJsonObject.get(language).getAsString,
        hit.get("description").getAsJsonObject.get(language).getAsString,
        hit.get("articleApiId").getAsLong,
        labels.map(x => api.Label(Option(x.get("type")).map(_.getAsString), x.get("labels").getAsJsonArray.asScala.toSeq.map(_.getAsString))).toSeq
      )
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
      logger.info(s"executing query:\n$queryBuilder")
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
        case (Sort.ByTitleAsc) => SortBuilders.fieldSort(s"title.$language.raw").setNestedPath("title").order(SortOrder.ASC).missing("_last")
        case (Sort.ByTitleDesc) => SortBuilders.fieldSort(s"title.$language.raw").setNestedPath("title").order(SortOrder.DESC).missing("_last")
        case (Sort.ByRelevanceAsc) => SortBuilders.fieldSort("_score").order(SortOrder.ASC)
        case (Sort.ByRelevanceDesc) => SortBuilders.fieldSort("_score").order(SortOrder.DESC)
        case (Sort.ByLastUpdatedAsc) => SortBuilders.fieldSort("lastUpdated").order(SortOrder.ASC).missing("_last")
        case (Sort.ByLastUpdatedDesc) => SortBuilders.fieldSort("lastUpdated").order(SortOrder.DESC).missing("_last")
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

      f onFailure { case t => logger.warn("Unable to create index: " + t.getMessage, t) }
      f onSuccess {
        case Success(reindexResult)  => logger.info(s"Completed indexing of ${reindexResult.totalIndexed} documents in ${reindexResult.millisUsed} ms.")
        case Failure(ex) => logger.warn(ex.getMessage, ex)
      }
    }
  }

}
