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
import io.searchbox.core.{Bulk, Delete, Index}
import io.searchbox.indices.aliases.{AddAliasMapping, GetAliases, ModifyAliases, RemoveAliasMapping}
import io.searchbox.indices.mapping.PutMapping
import io.searchbox.indices.{CreateIndex, DeleteIndex, IndicesExists}
import no.ndla.listingapi.ListingApiProperties
import no.ndla.listingapi.integration.{Elastic4sClient, ElasticClient}
import no.ndla.listingapi.model.domain.Cover
import no.ndla.listingapi.model.domain.search.Language.languageAnalyzers
import no.ndla.listingapi.model.domain.search.SearchableLanguageFormats
import org.elasticsearch.common.settings.Settings
import org.json4s.native.Serialization.write

import scala.util.{Failure, Success, Try}

trait IndexService {
  this: ElasticClient with Elastic4sClient with SearchConverterService =>
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
      val bulkBuilder = new Bulk.Builder()
      covers.foreach(cover => bulkBuilder.addAction(createIndexRequest(cover, indexName)))

      val response = jestClient.execute(bulkBuilder.build())
      response.map(r => {
        logger.info(s"Indexed ${covers.size} documents. No of failed items: ${r.getFailedItems.size()}")
        covers.size
      })
    }

    def deleteDocument(coverId: Long): Try[_] = {
      if (indexService.aliasTarget.isEmpty) {
        createIndexWithGeneratedName.map(createAliasTarget)
      }
      val deleteRequest = new Delete.Builder(s"$coverId")
        .index(ListingApiProperties.SearchIndex)
        .`type`(ListingApiProperties.SearchDocument).build()
      jestClient.execute(deleteRequest)
    }

    def createIndexWithGeneratedName: Try[String] = {
      createIndexWithName(ListingApiProperties.SearchIndex + "_" + getTimestamp)
    }

    def createIndexWithName(indexName: String): Try[String] = {
      if (indexExists(indexName)) {
        Success(indexName)
      } else {
        val settings = Map(
          s"analysis.analyzer.${labelAnalyzer.name}.type" -> "custom",
          s"analysis.analyzer.${labelAnalyzer.name}.tokenizer" -> "keyword",
          s"analysis.analyzer.${labelAnalyzer.name}.filter" -> "lowercase",
          s"index.max_result_window" -> ListingApiProperties.ElasticSearchIndexMaxResultWindow
        )

        val response = e4sClient.execute{
          createIndex(indexName)
            .mappings(buildMapping)
            .settings(settings)
        }

        response match {
          case Success(resp) => Success(indexName)
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
          textField("coverPhotoUrl").index(false),
          intField("articleApiId").index(false),
          intField("revision").index(false),
          textField("supportedLanguages").index(false),
          textField("updatedBy").index(false),
          dateField("update").index(false),
          textField("theme").index(false),
          intField("oldNodeId").index(false)
        )
    }

    private def languageSupportedLabels(fieldName: String) = {
      val languageMappings = languageAnalyzers.map(analyzer => {
        nestedField(analyzer.lang).fields(
          textField("type").fielddata(true).index(false),
          textField("labels").fielddata(true).analyzer(labelAnalyzer.name)
        )
      })
      nestedField(fieldName).fields(languageMappings)
    }

    private def languageSupportedField(fieldName: String, keepRaw: Boolean = false) = {
      NestedFieldDefinition(fieldName).fields(
        keepRaw match {
          case true => languageAnalyzers.map(langAnalyzer => textField(langAnalyzer.lang).fielddata(true).analyzer(langAnalyzer.analyzer).fields(keywordField("raw").index(false)))
          case false => languageAnalyzers.map(langAnalyzer => textField(langAnalyzer.lang).fielddata(true).analyzer(langAnalyzer.analyzer))
        }
      )

    }

    def aliasTarget: Option[String] = {
      val getAliasRequest = new GetAliases.Builder().addIndex(s"${ListingApiProperties.SearchIndex}").build()
      jestClient.execute(getAliasRequest) match {
        case Success(result) =>
          val aliasIterator = result.getJsonObject.entrySet().iterator()
          aliasIterator.hasNext match {
            case true => Some(aliasIterator.next().getKey)
            case false => None
          }
        case _ => None
      }
    }

    def createAliasTarget(indexName: String): Try[_] = {
      if (!indexExists(indexName)) {
        return Failure(new IllegalArgumentException(s"No such index: $indexName"))
      }

      val addAliasDefinition = new AddAliasMapping.Builder(indexName, ListingApiProperties.SearchIndex).build()
      jestClient.execute(new ModifyAliases.Builder(addAliasDefinition).build())
    }

    def updateAliasTarget(oldIndex: String, newIndexName: String): Try[_] = {
      if (!indexExists(newIndexName)) {
        return Failure(new IllegalArgumentException(s"No such index: $newIndexName"))
      }

      val addAliasDefinition = new AddAliasMapping.Builder(newIndexName, ListingApiProperties.SearchIndex).build()
      val modifyAliasRequest = new ModifyAliases.Builder(new RemoveAliasMapping.Builder(oldIndex, ListingApiProperties.SearchIndex).build())
        .addAlias(addAliasDefinition).build()

      jestClient.execute(modifyAliasRequest)
    }

    def deleteIndex(indexName: String): Try[_] = {
      if (!indexExists(indexName)) {
        Failure(new IllegalArgumentException(s"No such index: $indexName"))
      } else {
        jestClient.execute(new DeleteIndex.Builder(indexName).build())
      }
    }

    private def indexExists(indexName: String): Boolean = {
      jestClient.execute(new IndicesExists.Builder(indexName).build()).isSuccess
    }

    private def getTimestamp: String = {
      new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance.getTime)
    }
  }
}
