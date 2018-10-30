/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.listingapi.service.search

import java.nio.file.{Files, Path}

import com.sksamuel.elastic4s.embedded.{InternalLocalNode, LocalNode}
import no.ndla.listingapi.ListingApiProperties.{
  DefaultLanguage,
  DefaultPageSize
}
import no.ndla.listingapi.integration.{Elastic4sClientFactory, NdlaE4sClient}
import no.ndla.listingapi.model.domain.search.Sort
import no.ndla.listingapi.model.domain.{
  Description,
  Label,
  LanguageLabels,
  Title
}
import no.ndla.listingapi._
import no.ndla.tag.IntegrationTest
import org.joda.time.{DateTime, DateTimeZone}

import scala.util.{Failure, Success, Try}

class SearchServiceTest extends IntegrationSuite with TestEnvironment {
  val tmpDir: Path = Files.createTempDirectory(this.getClass.getName)
  val localNodeSettings: Map[String, String] =
    LocalNode.requiredSettings(this.getClass.getName, tmpDir.toString)
  val localNode: InternalLocalNode = LocalNode(localNodeSettings)
  override val e4sClient: NdlaE4sClient = NdlaE4sClient(localNode.client(true))

  override val searchService = new SearchService
  override val indexService = new IndexService
  override val searchConverterService = new SearchConverterService
  override val converterService = new ConverterService
  val today = DateTime.now()
  val date1 = new DateTime(2017, 2, 1, 12, 12, 32, DateTimeZone.UTC).toDate
  val date2 = new DateTime(2018, 1, 2, 15, 15, 32, DateTimeZone.UTC).toDate
  val date3 = new DateTime(2019, 3, 1, 12, 11, 32, DateTimeZone.UTC).toDate
  val date4 = new DateTime(2020, 4, 3, 11, 15, 32, DateTimeZone.UTC).toDate
  val date5 = new DateTime(2021, 5, 1, 1, 15, 32, DateTimeZone.UTC).toDate

  val cover1 = TestData.sampleCover.copy(
    id = Some(1),
    description = Seq(Description("stop. hammer time", "nb")),
    title = Seq(Title("hammer", "nb")),
    labels = Seq(
      LanguageLabels(Seq(Label(None, Seq("hammer", "time", "stop"))), "nb")),
    updated = date4,
    articleApiId = 1321
  )

  val cover2 = TestData.sampleCover.copy(
    id = Some(2),
    description = Seq(Description("forsiktig med saga", "nb")),
    title = Seq(Title("sag", "nb")),
    labels = Seq(
      LanguageLabels(Seq(Label(None, Seq("sag", "forsiktig", "farlig"))),
                     "nb")),
    updated = date2,
    articleApiId = 432
  )

  val cover3 = TestData.sampleCover.copy(
    id = Some(3),
    description = Seq(Description("her er døden selv", "nb")),
    title = Seq(Title("mannen med ljåen", "nb")),
    labels = Seq(
      LanguageLabels(
        Seq(Label(None,
                  Seq("ljå", "mann", "huff", "farlig", "personlig verktøy"))),
        "nb")),
    updated = date1,
    articleApiId = 896
  )

  val cover4 = TestData.sampleCover.copy(
    id = Some(4),
    description =
      Seq(Description("unrelated", "en"), Description("urelatert", "nb")),
    title = Seq(Title("unrelated", "en"), Title("urelatert", "nb")),
    labels = Seq(LanguageLabels(Seq(Label(None, Seq("pilt"))), "en"),
                 LanguageLabels(Seq(Label(None, Seq("pompel"))), "nb")),
    updated = date5,
    articleApiId = 512
  )

  val cover5 = TestData.sampleCover.copy(
    id = Some(5),
    description = Seq(Description("englando only", "en")),
    title = Seq(Title("englando", "en")),
    labels = Seq(LanguageLabels(Seq(Label(None, Seq("english"))), "en")),
    updated = date3,
    articleApiId = 134
  )

  override def beforeAll = {
    indexService.createIndexWithName(ListingApiProperties.SearchIndex)
    indexService.indexDocument(cover1)
    indexService.indexDocument(cover2)
    indexService.indexDocument(cover3)
    indexService.indexDocument(cover4)
    indexService.indexDocument(cover5)

    blockUntil(() => searchService.countDocuments == 5)
  }

  def blockUntil(predicate: () => Boolean): Unit = {
    0 to 16 foreach (backoff => {
      Thread.sleep(200 * backoff)

      Try(predicate()) match {
        case Success(done) if done => return
        case Failure(e)            => println("Problem while testing predicate", e)
        case _                     =>
      }
    })

    throw new IllegalArgumentException("Failed waiting for predicate")
  }

  test(
    "matchingQuery should return only covers with labels contained in the filter") {
    searchService
      .matchingQuery(Seq("hammer"), "nb", 1, 10, Sort.ByIdAsc)
      .results
      .map(_.id) should equal(Seq(1))
    searchService
      .matchingQuery(Seq("farlig"), "nb", 1, 10, Sort.ByIdAsc)
      .results
      .map(_.id) should equal(Seq(2, 3))
    searchService
      .matchingQuery(Seq("FARLIG", "hUFf"), "nb", 1, 10, Sort.ByIdAsc)
      .results
      .map(_.id) should equal(Seq(3))
    searchService
      .matchingQuery(Seq("FARLIG", "hUFf", "personlig verktøy"),
                     "nb",
                     1,
                     10,
                     Sort.ByIdAsc)
      .results
      .map(_.id) should equal(Seq(3))
    searchService
      .matchingQuery(Seq("FARLIG", "hUFf", "personlig"),
                     "nb",
                     1,
                     10,
                     Sort.ByIdAsc)
      .results
      .map(_.id) should equal(Seq())
    searchService
      .matchingQuery(Seq("farlig", "sag", "stop"), "nb", 1, 10, Sort.ByIdAsc)
      .results
      .map(_.id) should equal(Seq())
  }

  test(
    "That getStartAtAndNumResults returns SEARCH_MAX_PAGE_SIZE for value greater than SEARCH_MAX_PAGE_SIZE") {
    searchService.getStartAtAndNumResults(0, 1000) should equal(
      (0, ListingApiProperties.MaxPageSize))
  }

  test(
    "That getStartAtAndNumResults returns the correct calculated start at for page and page-size with default page-size") {
    val page = 74
    val expectedStartAt = (page - 1) * DefaultPageSize
    searchService.getStartAtAndNumResults(page, DefaultPageSize) should equal(
      (expectedStartAt, DefaultPageSize))
  }

  test(
    "That getStartAtAndNumResults returns the correct calculated start at for page and page-size") {
    val page = 123
    val expectedStartAt = (page - 1) * DefaultPageSize
    searchService.getStartAtAndNumResults(page, DefaultPageSize) should equal(
      (expectedStartAt, DefaultPageSize))
  }

  test("That all returns all documents ordered by id ascending") {
    val results =
      searchService.all(DefaultLanguage, 1, DefaultPageSize, Sort.ByIdAsc)
    results.totalCount should be(4)
    results.results.head.id should be(1)
    results.results(1).id should be(2)
    results.results(2).id should be(3)
    results.results(3).id should be(4)
  }

  test("That all returns all documents ordered by id descending") {
    val results =
      searchService.all(DefaultLanguage, 1, DefaultPageSize, Sort.ByIdDesc)
    results.totalCount should be(4)
    results.results.head.id should be(4)
    results.results.last.id should be(1)
  }

  test(
    "That paging returns only hits on current page and not more than page-size") {
    val page1 = searchService.all(DefaultLanguage, 1, 2, Sort.ByIdAsc)
    val page2 = searchService.all(DefaultLanguage, 2, 2, Sort.ByIdAsc)
    page1.totalCount should be(4)
    page1.page should be(1)
    page1.results.size should be(2)
    page1.results.head.id should be(1)
    page1.results.last.id should be(2)
    page2.totalCount should be(4)
    page2.page should be(2)
    page2.results.size should be(2)
    page2.results.head.id should be(3)
    page2.results.last.id should be(4)
  }

  test("That search returns covers sorted by title ascending") {
    val results = searchService.all("all", 1, 10, Sort.ByTitleAsc)

    results.totalCount should be(5)
    results.results.toList.map(_.id) should be(Seq(5, 1, 3, 2, 4))
  }

  test("That search returns covers sorted by title descending") {
    val results = searchService.all("all", 1, 10, Sort.ByTitleDesc)

    results.totalCount should be(5)
    results.results.toList.map(_.id) should be(Seq(4, 2, 3, 1, 5))
  }

  test("That search returns covers sorted by lastUpdated ascending") {
    val results = searchService.all("all", 1, 10, Sort.ByLastUpdatedAsc)

    results.totalCount should be(5)
    results.results.map(_.id) should be(Seq(3, 2, 5, 1, 4))
  }

  test("That search returns covers sorted by lastUpdated descending") {
    val results = searchService.all("all", 1, 10, Sort.ByLastUpdatedDesc)

    results.totalCount should be(5)
    results.results.map(_.id) should be(Seq(4, 1, 5, 2, 3))
  }

  test(
    "That search returns matched language when filtering and searching for all languages") {
    val resultsEn =
      searchService.matchingQuery(Seq("pilt"), "all", 1, 10, Sort.ByIdAsc)
    val resultsNb =
      searchService.matchingQuery(Seq("pompel"), "all", 1, 10, Sort.ByTitleAsc)

    resultsEn.totalCount should be(1)
    resultsEn.results.head.id should be(4)
    resultsEn.results.head.title.language should be("en")

    resultsNb.totalCount should be(1)
    resultsNb.results.head.id should be(4)
    resultsNb.results.head.title.language should be("nb")
  }

  test("That searching for all languages returns 'best' language") {
    val results = searchService.all("all", 1, 10, Sort.ByTitleDesc)

    results.totalCount should be(5)
    results.results.map(_.id) should be(Seq(4, 2, 3, 1, 5))
    results.results(0).title.language should be("nb")
    results.results(4).title.language should be("en")
  }

  override def afterAll = {
    indexService.deleteIndexWithName(Some(ListingApiProperties.SearchIndex))
  }
}
