/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.listingapi.service.search

import java.text.SimpleDateFormat
import java.util.Calendar

import com.sksamuel.elastic4s.analyzers._
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.indexes.IndexDefinition
import com.sksamuel.elastic4s.mappings.{MappingDefinition, NestedFieldDefinition}
import com.typesafe.scalalogging.LazyLogging
import no.ndla.listingapi.ListingApiProperties
import no.ndla.listingapi.integration.Elastic4sClient
import no.ndla.listingapi.model.domain.Cover
import no.ndla.listingapi.model.domain.search.Language.languageAnalyzers
import no.ndla.listingapi.model.domain.search.SearchableLanguageFormats
import org.elasticsearch.common.settings.Settings
import org.json4s.native.Serialization.write

import scala.util.{Failure, Success, Try}

trait IndexService {
  this: Elastic4sClient with SearchConverterService =>
  val indexService: IndexService

  class IndexService extends LazyLogging {
    val labelAnalyzer = CustomAnalyzer("lowercaseKeyword")

    private def createIndexRequest(card: Cover, indexName: String): IndexDefinition = {
      implicit val formats = SearchableLanguageFormats.JSonFormats
      val source = write(searchConverterService.asSearchableCover(card))
      indexInto(indexName / ListingApiProperties.SearchDocument).doc(source).id(card.id.get.toString)
    }

    def indexDocument(cover: Cover): Try[Cover] = {
      e4sClient.execute{
        createIndexRequest(cover, ListingApiProperties.SearchIndex)
      } match {
        case Success(_) => Success(cover)
        case Failure(ex) => Failure(ex)
      }
    }

    def indexDocuments(covers: List[Cover], indexName: String): Try[Int] = {
      if (covers.isEmpty) {
        Success(0)
      }
      else {
        val response = e4sClient.execute {
          bulk(covers.map(cover => {
            createIndexRequest(cover, indexName)
          }))
        }

        response match {
          case Success(res) =>
            logger.info(s"Indexed ${covers.size} documents. No of failed items: ${res.result.failures.size}")
            Success(covers.size)
          case Failure(ex) => Failure(ex)
        }
      }
    }

    def deleteDocument(coverId: Long): Try[_] = {
      for {
        _ <- aliasTarget.map {
          case Some(index) => Success(index)
          case None => createIndexWithGeneratedName.map(newIndex => updateAliasTarget(None, newIndex))
        }
        deleted <- {
          e4sClient.execute{
            delete(s"$coverId").from(ListingApiProperties.SearchIndex / ListingApiProperties.SearchDocument)
          }
        }
      } yield deleted
    }

    def createIndexWithGeneratedName: Try[String] = {
      createIndexWithName(ListingApiProperties.SearchIndex + "_" + getTimestamp)
    }

    def createIndexWithName(indexName: String): Try[String] = {
      if (indexWithNameExists(indexName).getOrElse(false)) {
        Success(indexName)
      } else {
        val settings = Map(
          s"analysis.analyzer.${labelAnalyzer.name}.type" -> "custom",
          s"analysis.analyzer.${labelAnalyzer.name}.tokenizer" -> "keyword",
          s"analysis.analyzer.${labelAnalyzer.name}.filter" -> "lowercase",
          s"max_result_window" -> ListingApiProperties.ElasticSearchIndexMaxResultWindow
        )

        val response = e4sClient.execute{
          createIndex(indexName)
            .mappings(buildMapping)
            .settings(settings)
        }

        response match {
          case Success(_) => Success(indexName)
          case Failure(ex) => Failure(ex)
        }
      }
    }

    private def buildMapping: MappingDefinition = {
      mapping(ListingApiProperties.SearchDocument).fields(
          intField("id"),
          languageSupportedField("title", keepRaw = true),
          languageSupportedField("description"),
          languageSupportedLabels("labels"),
          keywordField("defaultTitle"),
          textField("coverPhotoUrl"),
          intField("articleApiId"),
          intField("revision"),
          textField("supportedLanguages"),
          textField("updatedBy"),
          dateField("lastUpdated"),
          textField("theme"),
          intField("oldNodeId")
        )
    }

    private def languageSupportedLabels(fieldName: String) = {
      val languageMappings = languageAnalyzers.map(analyzer => {
        nestedField(analyzer.lang).fields(
          textField("type").fielddata(true),
          textField("labels").fielddata(true).analyzer(labelAnalyzer.name)
        )
      })
      nestedField(fieldName).fields(languageMappings)
    }

    private def languageSupportedField(fieldName: String, keepRaw: Boolean = false) = {
      NestedFieldDefinition(fieldName).fields(
        keepRaw match {
          case true => languageAnalyzers.map(langAnalyzer => textField(langAnalyzer.lang).fielddata(true).analyzer(langAnalyzer.analyzer).fields(keywordField("raw")))
          case false => languageAnalyzers.map(langAnalyzer => textField(langAnalyzer.lang).fielddata(true).analyzer(langAnalyzer.analyzer))
        }
      )

    }

    def aliasTarget: Try[Option[String]] = {
      val response = e4sClient.execute{
        getAliases(Nil, List(ListingApiProperties.SearchIndex))
      }

      response match {
        case Success(results) =>
          Success(results.result.mappings.headOption.map((t) => t._1.name))
        case Failure(ex) => Failure(ex)
      }
    }

    def updateAliasTarget(oldIndexName: Option[String], newIndexName: String): Try[Any] = {
      if (!indexWithNameExists(newIndexName).getOrElse(false)) {
        Failure(new IllegalArgumentException(s"No such index: $newIndexName"))
      } else {
        oldIndexName match {
          case None =>
            e4sClient.execute(addAlias(ListingApiProperties.SearchIndex).on(newIndexName))
          case Some(oldIndex) =>
            e4sClient.execute {
              removeAlias(ListingApiProperties.SearchIndex).on(oldIndex)
              addAlias(ListingApiProperties.SearchIndex).on(newIndexName)
            }
        }
      }
    }

    def deleteIndexWithName(optIndexName: Option[String]): Try[_] = {
      optIndexName match {
        case None => Success(optIndexName)
        case Some(indexName) => {
          if (!indexWithNameExists(indexName).getOrElse(false)) {
            Failure(new IllegalArgumentException(s"No such index: $indexName"))
          } else {
            e4sClient.execute{
              deleteIndex(indexName)
            }
          }
        }
      }
    }

    private def indexWithNameExists(indexName: String): Try[Boolean] = {
      val response = e4sClient.execute {
        indexExists(indexName)
      }

      response match {
        case Success(resp) if resp.status != 404 => Success(true)
        case Success(_) => Success(false)
        case Failure(ex) => Failure(ex)
      }
    }

    private def getTimestamp: String = {
      new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance.getTime)
    }
  }
}
