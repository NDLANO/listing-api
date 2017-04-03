package no.ndla.listingapi.repository

import no.ndla.listingapi.model.domain
import no.ndla.listingapi.{DBMigrator, IntegrationSuite, TestData, TestEnvironment}
import scalikejdbc.{ConnectionPool, DataSourceConnectionPool}

class ListingRepositoryTest extends IntegrationSuite with TestEnvironment {
  var repository: ListingRepository = _

  override def beforeEach = {
    repository = new ListingRepository()
  }

  override def beforeAll = {
    val dataSource = getDataSource
    DBMigrator.migrate(dataSource)
    ConnectionPool.singleton(new DataSourceConnectionPool(dataSource))
  }

  val sampleCover: domain.Cover = TestData.sampleCover

  test("inserting a new cover should return the new ID") {
    val result = repository.newCover(sampleCover)
    result.id.isDefined should be (true)

    repository.deleteCover(result.id.get)
  }

  test("getCover should return a cover") {
    val inserted = repository.newCover(sampleCover)
    inserted.id.isDefined should be (true)

    repository.getCover(inserted.id.get) should equal (Some(inserted))

    repository.deleteCover(inserted.id.get)
  }

  test("updateing a new cover should return the cover on success") {
    val inserted = repository.newCover(sampleCover)
    val toUpdate = inserted.copy(articleApiId = inserted.articleApiId + 1)

    val result = repository.updateCover(toUpdate)
    result.isSuccess should be (true)
    result.get.articleApiId should equal (toUpdate.articleApiId)
    result.get.id.isDefined should be (true)

    repository.deleteCover(result.get.id.get)
  }

  test("updateing a new cover should return a failure if failed to update cover") {
    repository.updateCover(TestData.sampleCover).isFailure should be (true)
  }
}
