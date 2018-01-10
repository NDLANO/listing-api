/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.listingapi.service.search

import no.ndla.listingapi.ListingApiProperties.{DefaultLanguage, DefaultPageSize}
import no.ndla.listingapi.integration.{Elastic4sClientFactory, JestClientFactory}
import no.ndla.listingapi.model.domain.search.Sort
import no.ndla.listingapi.model.domain.{Description, Label, LanguageLabels, Title}
import no.ndla.listingapi.{ListingApiProperties, TestData, TestEnvironment, UnitSuite}
import no.ndla.tag.IntegrationTest
import org.joda.time.DateTime

import scala.util.{Failure, Success, Try}

@IntegrationTest
class SearchServiceTest extends UnitSuite with TestEnvironment {

  val esPort = 9200
  override val jestClient = JestClientFactory.getClient(searchServer = s"http://localhost:$esPort")
  override val e4sClient = Elastic4sClientFactory.getClient(searchServer = s"http://localhost:$esPort")
  override val searchService = new SearchService
  override val indexService = new IndexService
  override val searchConverterService = new SearchConverterService
  val today = DateTime.now()

  val cover1 = TestData.sampleCover.copy(
    id = Some(1),
    description = Seq(Description("stop. hammer time", "nb")),
    title = Seq(Title("hammer", "nb")),
    labels = Seq(LanguageLabels(Seq(Label(None, Seq("hammer", "time", "stop"))), "nb")),
    articleApiId = 1321)

  val cover2 = TestData.sampleCover.copy(
    id = Some(2),
    description = Seq(Description("forsiktig med saga", "nb")),
    title = Seq(Title("sag", "nb")),
    labels = Seq(LanguageLabels(Seq(Label(None, Seq("sag", "forsiktig", "farlig"))), "nb")),
    articleApiId = 432)

  val cover3 = TestData.sampleCover.copy(
    id = Some(3),
    description = Seq(Description("her er døden selv", "nb")),
    title = Seq(Title("mannen med ljåen", "nb")),
    labels = Seq(LanguageLabels(Seq(Label(None, Seq("ljå", "mann", "huff", "farlig", "personlig verktøy"))), "nb")),
    articleApiId = 896)

  override def beforeAll = {
    indexService.createIndexWithName(ListingApiProperties.SearchIndex)
    indexService.indexDocument(cover1)
    indexService.indexDocument(cover2)
    indexService.indexDocument(cover3)

    blockUntil(() => searchService.countDocuments == 3)
  }

  def blockUntil(predicate: () => Boolean): Unit = {
    0 to 16 foreach (backoff => {
      Thread.sleep(200 * backoff)

      Try(predicate()) match {
        case Success(done) if done => return
        case Failure(e) => println("Problem while testing predicate", e)
        case _ =>
      }
    })

    throw new IllegalArgumentException("Failed waiting for predicate")
  }

  test("matchingQuery should return only covers with labels contained in the filter") {
    searchService.matchingQuery(Seq("hammer"), "nb", 1, 10, Sort.ByIdAsc).results.map(_.id) should equal(Seq(1))
    searchService.matchingQuery(Seq("farlig"), "nb", 1, 10, Sort.ByIdAsc).results.map(_.id) should equal(Seq(2, 3))
    searchService.matchingQuery(Seq("FARLIG", "hUFf"), "nb", 1, 10, Sort.ByIdAsc).results.map(_.id) should equal(Seq(3))
    searchService.matchingQuery(Seq("FARLIG", "hUFf", "personlig verktøy"), "nb", 1, 10, Sort.ByIdAsc).results.map(_.id) should equal(Seq(3))
    searchService.matchingQuery(Seq("FARLIG", "hUFf", "personlig"), "nb", 1, 10, Sort.ByIdAsc).results.map(_.id) should equal(Seq())
    searchService.matchingQuery(Seq("farlig", "sag", "stop"), "nb", 1, 10, Sort.ByIdAsc).results.map(_.id) should equal(Seq())
  }

  test("That getStartAtAndNumResults returns SEARCH_MAX_PAGE_SIZE for value greater than SEARCH_MAX_PAGE_SIZE") {
    searchService.getStartAtAndNumResults(0, 1000) should equal((0, ListingApiProperties.MaxPageSize))
  }

  test("That getStartAtAndNumResults returns the correct calculated start at for page and page-size with default page-size") {
    val page = 74
    val expectedStartAt = (page - 1) * DefaultPageSize
    searchService.getStartAtAndNumResults(page, DefaultPageSize) should equal((expectedStartAt, DefaultPageSize))
  }

  test("That getStartAtAndNumResults returns the correct calculated start at for page and page-size") {
    val page = 123
    val expectedStartAt = (page - 1) * DefaultPageSize
    searchService.getStartAtAndNumResults(page, DefaultPageSize) should equal((expectedStartAt, DefaultPageSize))
  }

  test("That all returns all documents ordered by id ascending") {
    val results = searchService.all(DefaultLanguage, 1, DefaultPageSize, Sort.ByIdAsc)
    results.totalCount should be(3)
    results.results.head.id should be(1)
    results.results(1).id should be(2)
    results.results(2).id should be(3)
  }

  test("That all returns all documents ordered by id descending") {
    val results = searchService.all(DefaultLanguage, 1, DefaultPageSize, Sort.ByIdDesc)
    results.totalCount should be(3)
    results.results.head.id should be(3)
    results.results.last.id should be(1)
  }


  test("That paging returns only hits on current page and not more than page-size") {
    val page1 = searchService.all(DefaultLanguage, 1, 2, Sort.ByIdAsc)
    val page2 = searchService.all(DefaultLanguage, 2, 2, Sort.ByIdAsc)
    page1.totalCount should be(3)
    page1.page should be(1)
    page1.results.size should be(2)
    page1.results.head.id should be(1)
    page1.results.last.id should be(2)
    page2.totalCount should be(3)
    page2.page should be(2)
    page2.results.size should be(1)
    page2.results.head.id should be(3)
  }

  //TODO: test titlesorting
  //TODO: test relevance sorting
  //TODO: test last updated sorting?
  //TODO: test sort by lastUpdated?

  //TODO: test searching for all languages returns matched language
  //TODO: test searching for all languages returns multiple languages
  //TODO: test searching for all languages with sort?

  override def afterAll = {
    indexService.deleteIndexWithName(Some(ListingApiProperties.SearchIndex))
  }
}
