/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.listingapi.service.search

import com.sksamuel.elastic4s.searches.queries.BoolQueryDefinition
import com.typesafe.scalalogging.LazyLogging
import no.ndla.listingapi.ListingApiProperties
import no.ndla.listingapi.ListingApiProperties.{MaxPageSize, SearchIndex}
import no.ndla.listingapi.integration.{Elastic4sClient, ElasticClient}
import no.ndla.listingapi.model.api
import no.ndla.listingapi.model.api.{NdlaSearchException, ResultWindowTooLargeException}
import no.ndla.listingapi.model.domain.search.{Language, Sort}
import no.ndla.listingapi.service.{Clock, createOembedUrl}
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.search.SearchResponse
import com.sksamuel.elastic4s.searches.ScoreMode
import com.sksamuel.elastic4s.searches.sort.{FieldSortDefinition, SortOrder}
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.index.IndexNotFoundException

import scala.collection.JavaConverters._
import org.json4s._
import org.json4s.native.JsonMethods._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

trait SearchService {
  this: ElasticClient with Elastic4sClient with SearchIndexService with SearchConverterService with Clock =>
  val searchService: SearchService

  class SearchService extends LazyLogging {
    implicit val formats = org.json4s.DefaultFormats

    private val noCopyright = boolQuery().not(termQuery("license", "copyrighted"))

    def all(language: String, page: Int, pageSize: Int, sort: Sort.Value): api.SearchResult = {
      executeSearch(
        language,
        sort,
        page,
        pageSize,
        boolQuery())
    }

    private def executeSearch(language: String, sort: Sort.Value, page: Int, pageSize: Int, queryBuilder: BoolQueryDefinition): api.SearchResult = {
      val (languageFilter, searchLanguage) = language match {
        case Language.NoLanguage | Language.AllLanguages => (None, "*")
        case lang => (Some(nestedQuery("title", existsQuery(s"title.$lang")).scoreMode(ScoreMode.Avg)), lang)
      }

      val filters = List(languageFilter)
      val filteredSearch = queryBuilder.filter(filters.flatten)


      val (startAt, numResults) = getStartAtAndNumResults(page, pageSize)
      val requestedResultWindow = pageSize*page
      if(requestedResultWindow > ListingApiProperties.ElasticSearchIndexMaxResultWindow) {
        logger.info(s"Max supported results are ${ListingApiProperties.ElasticSearchIndexMaxResultWindow}, user requested $requestedResultWindow")
        throw new ResultWindowTooLargeException()
      }

      val json = e4sClient.c.show{
        search(SearchIndex)
          .query(filteredSearch)
          .size(numResults)
          .from(startAt)
          .sortBy(getSortDefinition(sort, searchLanguage))
      }

      e4sClient.execute {
        search(SearchIndex)
          .query(filteredSearch)
          .size(numResults)
          .from(startAt)
          .sortBy(getSortDefinition(sort, searchLanguage))
      } match {
        case Success(response) =>
          api.SearchResult(response.result.totalHits, page, numResults, searchLanguage, getHits(response.result, searchLanguage))
        case Failure(ex) => errorHandler(Failure(ex))
      }

    }

    def getHits(response: SearchResponse, language: String): Seq[api.Cover] = {
      response.totalHits match {
        case count if count > 0 =>
          val resultArray = response.hits.hits

          resultArray.flatMap(result => {
            val matchedLanguage = language match {
              case Language.AllLanguages | "*" =>
                searchConverterService.getLanguageFromHit(result).getOrElse(language)
              case _ => language
            }

            hitAsCard(result.sourceAsString, matchedLanguage)
          })
        case _ => Seq()
      }
    }

    def hitAsCard(hitString: String, language: String): Option[api.Cover] = {
      val hit = parse(hitString)

      val labelsOpt = (hit \ "labels" \ language).extract[Option[JArray]]
      def oldNodeIdOrNone = if ((hit \ "oldNodeId") == JNothing ) None else createOembedUrl((hit \ "oldNodeId").extract[Long])
      labelsOpt.map(labels => {
        val title = (hit \ "title" \ language).extract[String]
        val description = (hit \ "description" \ language).extract[String]

        val api_labels = labels.arr.map(label => {
          api.Label(
            (label \ "type").extract[Option[String]],
            (label \"labels").extract[Seq[String]]
          )
        })
        val supportedLanguages = (hit \ "supportedLanguages").extract[Seq[String]]

        val api_cover = api.Cover(
          (hit \ "id").extract[Long],
          (hit \ "revision").extract[Int],
          (hit \ "coverPhotoUrl").extract[String],
          api.CoverTitle(title, language),
          api.CoverDescription(description, language),
          (hit \ "articleApiId").extract[Long],
          api.CoverLabels(api_labels, language),
          supportedLanguages.toSet,
          (hit \ "updatedBy").extract[String],
          clock.toDate((hit \ "lastUpdated").extract[String]),
          (hit \ "theme").extract[String],
          oldNodeIdOrNone
        )
        api_cover //TODO: remove temp variable
      })
    }

    def getSortDefinition(sort: Sort.Value, language: String): FieldSortDefinition = {
      val sortLanguage = language match {
        case Language.NoLanguage => Language.DefaultLanguage
        case _ => language
      }

      sort match {
        case (Sort.ByTitleAsc) =>
          language match {
            case "*" | Language.AllLanguages => fieldSort("defaultTitle").order(SortOrder.ASC).missing("_last")
            case _ => fieldSort(s"title.$sortLanguage.raw").nestedPath("title").order(SortOrder.ASC).missing("_last")
          }
        case (Sort.ByTitleDesc) =>
          language match {
            case "*" | Language.AllLanguages => fieldSort("defaultTitle").order(SortOrder.DESC).missing("_last")
            case _ => fieldSort(s"title.$sortLanguage.raw").nestedPath("title").order(SortOrder.DESC).missing("_last")
          }
        case (Sort.ByLastUpdatedAsc) => fieldSort("lastUpdated").order(SortOrder.ASC).missing("_last")
        case (Sort.ByLastUpdatedDesc) => fieldSort("lastUpdated").order(SortOrder.DESC).missing("_last")
        case (Sort.ByIdAsc) => fieldSort("id").order(SortOrder.ASC).missing("_last")
        case (Sort.ByIdDesc) => fieldSort("id").order(SortOrder.DESC).missing("_last")
      }
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

    def matchingQuery(query: Seq[String], language: String, page: Int, pageSize: Int, sort: Sort.Value): api.SearchResult = {
      val searchLanguage, languages = language match {
        case Language.AllLanguages | "*" =>
          Language.supportedLanguages
        case lang =>
          List(lang)
      }

      val queries = languages.map(lang => {
        boolQuery().must(query.map(q => {
          val hi = highlight("*").preTag("").postTag("").numberOfFragments(0)
          val ih = innerHits(lang).highlighting(hi)
          val termQ = matchPhraseQuery(s"labels.$lang.labels", q)
          val titleSearch = nestedQuery(s"labels.$lang", termQ).scoreMode(ScoreMode.Avg)
          nestedQuery("labels", titleSearch).scoreMode(ScoreMode.Avg).inner(ih)
        }))
      })

      val filter = boolQuery().should(queries)
      executeSearch(language, sort, page, pageSize, filter)
    }

    def countDocuments: Long = {
      val response = e4sClient.execute{
        catCount(SearchIndex)
      }

      response match {
        case Success(resp) => resp.result.count
        case Failure(_) => 0
      }
    }

  }

}
