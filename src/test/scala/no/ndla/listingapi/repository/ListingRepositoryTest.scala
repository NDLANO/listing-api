package no.ndla.listingapi.repository

import com.typesafe.scalalogging.LazyLogging
import no.ndla.listingapi.integration.DataSource
import no.ndla.listingapi.model.domain
import no.ndla.listingapi.model.meta.Theme
import no.ndla.listingapi.{
  DBMigrator,
  IntegrationSuite,
  TestData,
  TestEnvironment
}
import no.ndla.tag.IntegrationTest
import scalikejdbc.{ConnectionPool, DataSourceConnectionPool}

@IntegrationTest
class ListingRepositoryTest
    extends IntegrationSuite
    with TestEnvironment
    with LazyLogging {
  var repository: ListingRepository = _

  override def beforeEach: Unit = {
    repository = new ListingRepository()
  }

  override def beforeAll: Unit = {
    val dataSource = DataSource.getHikariDataSource
    DBMigrator.migrate(dataSource)
    ConnectionPool.singleton(new DataSourceConnectionPool(dataSource))
  }

  override def afterEach(): Unit = {
    //Simple way of just dumping the content of the DB for clean next test
    repository.allCovers().foreach(c => repository.deleteCover(c.id.get))
  }

  val sampleCover: domain.Cover = TestData.sampleCover
  val sampleCover2: domain.Cover = TestData.sampleCover2

  test("inserting a new cover should return the new ID") {
    val result = repository.insertCover(sampleCover)
    result.id.isDefined should be(true)
  }

  test("getCover should return a cover") {
    val inserted = repository.insertCover(sampleCover)
    inserted.id.isDefined should be(true)

    repository.getCover(inserted.id.get) should equal(Some(inserted))
  }

  test("updateing a new cover should return the cover on success") {
    val inserted = repository.insertCover(sampleCover)
    val toUpdate = inserted.copy(articleApiId = inserted.articleApiId + 1)

    val result = repository.updateCover(toUpdate)
    result.isSuccess should be(true)
    result.get.articleApiId should equal(toUpdate.articleApiId)
    result.get.id.isDefined should be(true)
  }

  test(
    "updateing a new cover should return a failure if failed to update cover") {
    repository.updateCover(TestData.sampleCover).isFailure should be(true)
  }

  test("newCover should set revision number to 1") {
    val result = repository.insertCover(sampleCover.copy(revision = None))
    result.id.isDefined should be(true)
    result.revision should be(Some(1))
  }

  test(
    "updateCover should fail to update if revision number does not match current") {
    val initial = repository.insertCover(sampleCover.copy(revision = None))
    repository
      .updateCover(initial.copy(revision = Some(initial.revision.get + 1)))
      .isFailure should be(true)
    repository
      .updateCover(initial.copy(revision = Some(initial.revision.get - 1)))
      .isFailure should be(true)
  }

  test("updateCover should update current revision number") {
    val initial = repository.insertCover(sampleCover.copy(revision = None))
    val firstUpdate = repository.updateCover(initial)

    firstUpdate.isSuccess should be(true)
    firstUpdate.get.revision.get should be(initial.revision.get + 1)

    val secondUpdate = repository.updateCover(firstUpdate.get)
    secondUpdate.isSuccess should be(true)
    secondUpdate.get.revision.get should be(initial.revision.get + 2)
  }

  test("get allLabels") {
    repository.insertCover(sampleCover)
    repository.insertCover(sampleCover2)
    repository.insertCover(sampleCover)
    repository.insertCover(sampleCover2)
    val allCovers = repository.allCovers()

    val allLabelsMap = repository.allLabelsMap()
    val allLabelsNB = allLabelsMap("nb")
    val allLabelsNN = allLabelsMap("nn")
    val allLabelsEN = allLabelsMap("en")

    allLabelsNB.labelsByType should be(
      Map("kategori" -> Set("bygg verktøy",
                            "jobbe verktøy",
                            "mer label",
                            "personlig verktøy"),
          "other" -> Set("bygg", "byggherrer")))
    allLabelsNN.labelsByType should be(
      Map("kategori" -> Set("arbeids verktøy"), "other" -> Set("byggkarer")))
    allLabelsEN.labelsByType should be(
      Map("category" -> Set("work tools"), "other" -> Set("workmen")))
  }

  test("getTheme should return sequence of cards given allowed named theme") {
    val cover1 = repository.insertCover(sampleCover)
    val cover2 = repository.insertCover(sampleCover2)

    val covers = repository.getTheme(Theme.VERKTOY)
    val allCovers = repository.allCovers()
    println(s"all ${allCovers.length} $allCovers")
    println("repository.getTheme(Theme.VERKTOY):\n", covers)
    covers.length should be(2)
    repository.getTheme(Theme.NATURBRUK).length should be(0)

  }

  test("getTheme should return 0 on valid theme") {
    val cover1 = repository.insertCover(sampleCover)
    val cover2 = repository.insertCover(sampleCover2)

    repository.getTheme("notvalid").length should be(0)
  }

}
