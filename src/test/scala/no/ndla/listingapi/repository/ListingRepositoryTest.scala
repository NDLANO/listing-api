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

  val sampleCard: domain.Card = TestData.sampleCard

  test("inserting a new cards should return the new ID") {
    val result = repository.insertCard(sampleCard)
    result.id.isDefined should be (true)

    repository.deleteCard(result.id.get)
  }

  test("getCard should return a card") {
    val inserted = repository.insertCard(sampleCard)
    inserted.id.isDefined should be (true)

    repository.getCard(inserted.id.get) should equal (Some(inserted))

    repository.deleteCard(inserted.id.get)
  }

}
