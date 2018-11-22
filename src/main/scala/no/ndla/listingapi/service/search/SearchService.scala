/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.listingapi.service.search

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.search.SearchResponse
import com.sksamuel.elastic4s.searches.ScoreMode
import com.sksamuel.elastic4s.searches.queries.BoolQuery
import com.sksamuel.elastic4s.searches.sort.{FieldSort, SortOrder}
import com.typesafe.scalalogging.LazyLogging
import no.ndla.listingapi.ListingApiProperties
import no.ndla.listingapi.ListingApiProperties.{MaxPageSize, SearchIndex}
import no.ndla.listingapi.integration.Elastic4sClient
import no.ndla.listingapi.model.{api, domain}
import no.ndla.listingapi.model.api.{NdlaSearchException, ResultWindowTooLargeException}
import no.ndla.listingapi.model.domain.search._
import no.ndla.listingapi.service.{Clock, ConverterService, createOembedUrl}
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.index.IndexNotFoundException
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

trait SearchService {
  this: Elastic4sClient with SearchIndexService with SearchConverterService with ConverterService with Clock =>
  val searchService: SearchService

  class SearchService extends LazyLogging {
    implicit val formats = SearchableLanguageFormats.JSonFormats

    private val noCopyright =
      boolQuery().not(termQuery("license", "copyrighted"))

    def all(language: String, page: Int, pageSize: Int, sort: Sort.Value): api.SearchResult = {
      executeSearch(
        language,
        sort,
        page,
        pageSize,
        List()
      )
    }

    def matchingQuery(query: Seq[String],
                      language: String,
                      page: Int,
                      pageSize: Int,
                      sort: Sort.Value): api.SearchResult = {
      val languages = language match {
        case Language.AllLanguages | "*" => Language.supportedLanguages
        case lang                        => List(lang)
      }

      val queries = languages.map(lang => {
        boolQuery().must(query.map(q => {
          val hi = highlight("*").preTag("").postTag("").numberOfFragments(0)
          val ih = innerHits(lang).highlighting(hi)
          val termQ = matchPhraseQuery(s"labels.$lang.labels", q)
          val titleSearch =
            nestedQuery(s"labels.$lang", termQ).scoreMode(ScoreMode.Avg)
          nestedQuery("labels", titleSearch).scoreMode(ScoreMode.Avg).inner(ih)
        }))
      })

      executeSearch(language, sort, page, pageSize, queries)
    }

    private def executeSearch(language: String,
                              sort: Sort.Value,
                              page: Int,
                              pageSize: Int,
                              queries: Seq[BoolQuery]): api.SearchResult = {
      val (languageFilter, searchLanguage) = language match {
        case Language.NoLanguage | Language.AllLanguages => (None, "*")
        case lang =>
          (Some(
             nestedQuery("title", existsQuery(s"title.$lang"))
               .scoreMode(ScoreMode.Avg)),
           lang)
      }

      val postFilters = List(languageFilter)
      val querySearch = boolQuery().should(queries)

      val (startAt, numResults) = getStartAtAndNumResults(page, pageSize)
      val requestedResultWindow = pageSize * page
      if (requestedResultWindow > ListingApiProperties.ElasticSearchIndexMaxResultWindow) {
        logger.info(
          s"Max supported results are ${ListingApiProperties.ElasticSearchIndexMaxResultWindow}, user requested $requestedResultWindow")
        throw new ResultWindowTooLargeException()
      }

      e4sClient.execute {
        search(SearchIndex)
          .query(querySearch)
          .postFilter(boolQuery().filter(postFilters.flatten))
          .size(numResults)
          .highlighting(highlight("*"))
          .from(startAt)
          .sortBy(getSortDefinition(sort, searchLanguage))
      } match {
        case Success(response) =>
          api.SearchResult(response.result.totalHits,
                           page,
                           numResults,
                           if (searchLanguage == "*") Language.AllLanguages
                           else searchLanguage,
                           getHits(response.result, searchLanguage))
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
                searchConverterService
                  .getLanguageFromHit(result)
                  .getOrElse(language)
              case _ => language
            }

            hitAsCard(result.sourceAsString, matchedLanguage)
          })
        case _ => Seq()
      }
    }

    def hitAsCard(hitString: String, language: String): Option[api.Cover] = {
      val searchableCover = read[SearchableCover](hitString)

      val languageLabels = searchableCover.labels.languageValues.map(lv => {
        val domainLabels = lv.value.map(llv => {
          domain.Label(llv.`type`, llv.labels)
        })
        domain.LanguageLabels(domainLabels, lv.lang)
      })

      Language
        .findByLanguageOrBestEffort(languageLabels, language)
        .map(labels => {
          val apiLabels = labels.labels.map(converterService.toApiCoverLabel)

          val titles = searchableCover.title.languageValues.map(lv => domain.Title(lv.value, lv.lang))
          val descriptions = searchableCover.description.languageValues
            .map(lv => domain.Description(lv.value, lv.lang))

          val title = converterService.toApiCoverTitle(titles, language)
          val description =
            converterService.toApiCoverDescription(descriptions, language)

          api.Cover(
            searchableCover.id,
            searchableCover.revision,
            searchableCover.coverPhotoUrl,
            title,
            description,
            searchableCover.articleApiId,
            api.CoverLabels(apiLabels, labels.language),
            searchableCover.supportedLanguages,
            searchableCover.updatedBy,
            searchableCover.lastUpdated,
            searchableCover.theme,
            searchableCover.oldNodeId.flatMap(createOembedUrl)
          )
        })
    }

    def getSortDefinition(sort: Sort.Value, language: String): FieldSort = {
      val sortLanguage = language match {
        case Language.NoLanguage => Language.DefaultLanguage
        case _                   => language
      }

      sort match {
        case Sort.ByTitleAsc =>
          language match {
            case "*" | Language.AllLanguages =>
              fieldSort("defaultTitle").order(SortOrder.ASC).missing("_last")
            case _ =>
              fieldSort(s"title.$sortLanguage.raw")
                .nestedPath("title")
                .order(SortOrder.ASC)
                .missing("_last")
          }
        case Sort.ByTitleDesc =>
          language match {
            case "*" | Language.AllLanguages =>
              fieldSort("defaultTitle").order(SortOrder.DESC).missing("_last")
            case _ =>
              fieldSort(s"title.$sortLanguage.raw")
                .nestedPath("title")
                .order(SortOrder.DESC)
                .missing("_last")
          }
        case Sort.ByLastUpdatedAsc =>
          fieldSort("lastUpdated").order(SortOrder.ASC).missing("_last")
        case Sort.ByLastUpdatedDesc =>
          fieldSort("lastUpdated").order(SortOrder.DESC).missing("_last")
        case Sort.ByIdAsc =>
          fieldSort("id").order(SortOrder.ASC).missing("_last")
        case Sort.ByIdDesc =>
          fieldSort("id").order(SortOrder.DESC).missing("_last")
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
          e.rf.status match {
            case notFound: Int if notFound == 404 => {
              logger.error(s"Index $SearchIndex not found. Scheduling a reindex.")
              scheduleIndexDocuments()
              throw new IndexNotFoundException(s"Index $SearchIndex not found. Scheduling a reindex")
            }
            case _ => {
              logger.error(e.getMessage)
              throw new ElasticsearchException(s"Unable to execute search in $SearchIndex", e.getMessage)
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
        case Success(reindexResult) =>
          logger.info(
            s"Completed indexing of ${reindexResult.totalIndexed} documents in ${reindexResult.millisUsed} ms.")
        case Failure(ex) => logger.warn(ex.getMessage, ex)
      }
    }

    def countDocuments: Long = {
      val response = e4sClient.execute {
        catCount(SearchIndex)
      }

      response match {
        case Success(resp) => resp.result.count
        case Failure(_)    => 0
      }
    }

  }

}
