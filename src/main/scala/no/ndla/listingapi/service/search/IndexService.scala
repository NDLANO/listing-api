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
import com.sksamuel.elastic4s.mappings.{MappingContentBuilder, NestedFieldDefinition}
import com.typesafe.scalalogging.LazyLogging
import io.searchbox.core.{Bulk, Delete, Index}
import io.searchbox.indices.aliases.{AddAliasMapping, GetAliases, ModifyAliases, RemoveAliasMapping}
import io.searchbox.indices.mapping.PutMapping
import io.searchbox.indices.{CreateIndex, DeleteIndex, IndicesExists}
import no.ndla.listingapi.ListingApiProperties
import no.ndla.listingapi.integration.ElasticClient
import no.ndla.listingapi.model.domain.Cover
import no.ndla.listingapi.model.domain.search.Language.languageAnalyzers
import no.ndla.listingapi.model.domain.search.SearchableLanguageFormats
import org.elasticsearch.common.settings.Settings
import org.json4s.native.Serialization.write

import scala.util.{Failure, Success, Try}

trait IndexService {
  this: ElasticClient with SearchConverterService =>
  val indexService: IndexService

  class IndexService extends LazyLogging {
    val labelAnalyzer = CustomAnalyzer("lowercaseKeyword")

    private def createIndexRequest(card: Cover, indexName: String) = {
      implicit val formats = SearchableLanguageFormats.JSonFormats
      val source = write(searchConverterService.asSearchableCover(card))
      new Index.Builder(source).index(indexName).`type`(ListingApiProperties.SearchDocument).id(card.id.get.toString).build
    }

    def indexDocument(cover: Cover): Try[Cover] = {
      val result = jestClient.execute(createIndexRequest(cover, ListingApiProperties.SearchIndex))
      result.map(_ => cover)
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
        createIndex.map(createAliasTarget)
      }
      val deleteRequest = new Delete.Builder(s"$coverId")
        .index(ListingApiProperties.SearchIndex)
        .`type`(ListingApiProperties.SearchDocument).build()
      jestClient.execute(deleteRequest)
    }

    def createIndex: Try[String] = {
      createIndexWithName(ListingApiProperties.SearchIndex + "_" + getTimestamp)
    }

    def createIndexWithName(indexName: String): Try[String] = {
      if (indexExists(indexName)) {
        Success(indexName)
      } else {
        val analyserSettings = Settings.builder()
          .put(s"analysis.analyzer.${labelAnalyzer.name}.type", "custom")
          .put(s"analysis.analyzer.${labelAnalyzer.name}.tokenizer", "keyword")
          .put(s"analysis.analyzer.${labelAnalyzer.name}.filter", "lowercase")
          .put(s"index.max_result_window", ListingApiProperties.ElasticSearchIndexMaxResultWindow)
          .build().getAsMap

        val createIndexResponse = jestClient.execute(new CreateIndex.Builder(indexName).settings(analyserSettings).build())
        createIndexResponse.map(_ => createMapping(indexName)).map(_ => indexName)
      }
    }

    private def createMapping(indexName: String): Try[String] = {
      val mappingResponse = jestClient.execute(new PutMapping.Builder(indexName, ListingApiProperties.SearchDocument, buildMapping()).build())
      mappingResponse.map(_ => indexName)
    }

    private def buildMapping(): String = {
      MappingContentBuilder.buildWithName(mapping(ListingApiProperties.SearchDocument).fields(
          intField("id"),
          languageSupportedField("title", keepRaw = true),
          languageSupportedField("description"),
          languageSupportedLabels("labels"),
          textField("coverPhotoUrl") index "not_analyzed",
          intField("articleApiId") index "not_analyzed",
          intField("revision") index "not_analyzed",
          textField("supportedLanguages") index "not_analyzed",
          textField("updatedBy") index "not_analyzed",
          dateField("update") index "not_analyzed",
          textField("theme") index "not_analyzed",
          intField("oldNodeId") index "not_analyzed"
        ),
        ListingApiProperties.SearchDocument).string()
    }

    private def languageSupportedLabels(fieldName: String) = {
      val languageMappings = languageAnalyzers.map(analyzer => {
        nestedField(analyzer.lang).as(
          textField("type") fielddata true index "not_analyzed",
          textField("labels") fielddata true analyzer labelAnalyzer.name
        )
      })

      new NestedFieldDefinition(fieldName).as(languageMappings:_*)
    }

    private def languageSupportedField(fieldName: String, keepRaw: Boolean = false) = {
      val languageSupportedField = new NestedFieldDefinition(fieldName)
      languageSupportedField._fields = keepRaw match {
        case true => languageAnalyzers.map(langAnalyzer => textField(langAnalyzer.lang).fielddata(true) analyzer langAnalyzer.analyzer fields (keywordField("raw") index "not_analyzed"))
        case false => languageAnalyzers.map(langAnalyzer => textField(langAnalyzer.lang).fielddata(true) analyzer langAnalyzer.analyzer)
      }

      languageSupportedField
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
