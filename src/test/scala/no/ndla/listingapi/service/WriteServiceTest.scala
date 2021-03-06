package no.ndla.listingapi.service

import no.ndla.listingapi.caching.Memoize
import no.ndla.listingapi.model.api.{CoverAlreadyExistsException, Label, NewCover, ValidationException}
import no.ndla.listingapi.model.domain
import no.ndla.listingapi.model.domain._
import no.ndla.listingapi.{TestData, TestEnvironment, UnitSuite}
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import scalikejdbc.DBSession

import scala.util.{Failure, Success}

class WriteServiceTest extends UnitSuite with TestEnvironment {
  val service = new WriteService

  val sampleNewCover = TestData.sampleApiNewCover
  val sampleApiCover = TestData.sampleApiCover
  val sampleCover = TestData.sampleCover
  val sampleApiUpdateCover = TestData.sampleApiUpdateCover

  override def beforeAll() = {
    when(authClient.client_id()).thenReturn("content-import-client")
    when(clock.now()).thenReturn((new DateTime(2017, 4, 1, 12, 15, 32, DateTimeZone.UTC)).toDate)
    val targetMock = mock[Target]
    val memoizedTarget = new Memoize[Map[Lang, UniqeLabels]](Long.MaxValue, targetMock.targetMethod, false)
    when(readService.getAllLabelsMap).thenReturn(memoizedTarget)
  }

  override def beforeEach = {
    reset(listingRepository)
  }

  class Target {
    def targetMethod(): Map[Lang, UniqeLabels] = Map()
  }

  test("newCover should return Failure if validation fails") {
    when(coverValidator.validate(any[Cover]))
      .thenReturn(Failure(mock[ValidationException]))
    service.newCover(sampleNewCover).isFailure should be(true)
  }

  test("newCover should return Failure on failure to store cover") {
    when(converterService.toDomainCover(any[NewCover])).thenReturn(sampleCover)
    when(coverValidator.validate(any[Cover])).thenReturn(Success(sampleCover))
    when(listingRepository.insertCover(any[Cover])(any[DBSession]))
      .thenThrow(new RuntimeException())

    service.newCover(sampleNewCover).isFailure should be(true)
  }

  test("newCover should return Failure on failure to index cover") {
    when(converterService.toDomainCover(any[NewCover])).thenReturn(sampleCover)
    when(coverValidator.validate(any[Cover])).thenReturn(Success(sampleCover))
    when(listingRepository.insertCover(any[Cover])(any[DBSession]))
      .thenReturn(sampleCover)
    when(indexService.indexDocument(sampleCover))
      .thenReturn(Failure(new RuntimeException()))

    service.newCover(sampleNewCover).isFailure should be(true)
  }

  test("newCover should return Success when everything is fine") {
    when(converterService.toDomainCover(any[NewCover])).thenReturn(sampleCover)
    when(coverValidator.validate(any[Cover])).thenReturn(Success(sampleCover))
    when(listingRepository.insertCover(any[Cover])(any[DBSession]))
      .thenReturn(sampleCover)
    when(indexService.indexDocument(sampleCover))
      .thenReturn(Success(sampleCover))
    when(converterService.toApiCover(sampleCover, "nb"))
      .thenReturn(sampleApiCover)

    service.newCover(sampleNewCover).isSuccess should be(true)
  }

  test("newCover should throw an exception if card already exists") {
    val newCover = sampleNewCover.copy(oldNodeId = Some(1))
    when(listingRepository.getCoverWithOldNodeId(1))
      .thenReturn(Some(sampleCover))

    assertThrows[CoverAlreadyExistsException] {
      service.newCover(newCover)
    }
  }

  test("updateCover Failure if cover was not found") {
    when(listingRepository.getCover(any[Long])(any[DBSession])).thenReturn(None)
    service.updateCover(1, sampleApiUpdateCover).isFailure should be(true)
  }

  test("updateCover should return Failure if validation fails") {
    when(coverValidator.validate(any[Cover]))
      .thenReturn(Failure(mock[ValidationException]))
    when(listingRepository.getCover(any[Long])(any[DBSession]))
      .thenReturn(Some(sampleCover))
    service.updateCover(1, sampleApiUpdateCover).isFailure should be(true)
  }

  test("updateCover should return Failure on failure to store cover") {
    when(coverValidator.validate(any[Cover]))
      .thenReturn(Failure(mock[ValidationException]))
    when(listingRepository.getCover(any[Long])(any[DBSession]))
      .thenReturn(Some(sampleCover))
    when(listingRepository.updateCover(any[Cover])(any[DBSession]))
      .thenThrow(new RuntimeException)

    service.updateCover(1, sampleApiUpdateCover).isFailure should be(true)
  }

  test("updateCover should return Failure on failure to index cover") {
    when(coverValidator.validate(any[Cover]))
      .thenReturn(Failure(mock[ValidationException]))
    when(listingRepository.getCover(any[Long])(any[DBSession]))
      .thenReturn(Some(sampleCover))
    when(listingRepository.updateCover(any[Cover])(any[DBSession]))
      .thenReturn(Success(sampleCover))
    when(indexService.indexDocument(sampleCover))
      .thenReturn(Failure(new RuntimeException))

    service.updateCover(1, sampleApiUpdateCover).isFailure should be(true)
  }

  test("updateCover should return Success if everything is fine") {
    when(coverValidator.validate(any[Cover]))
      .thenReturn(Failure(mock[ValidationException]))
    when(listingRepository.getCover(any[Long])(any[DBSession]))
      .thenReturn(Some(sampleCover))
    when(listingRepository.updateCover(any[Cover])(any[DBSession]))
      .thenReturn(Success(sampleCover))
    when(indexService.indexDocument(sampleCover))
      .thenReturn(Success(sampleCover))

    service.updateCover(1, sampleApiUpdateCover).isFailure should be(true)
  }

  test("mergeCovers should append a new language if language not already exists") {
    val toUpdate = sampleApiUpdateCover.copy(
      language = "en",
      title = "titl",
      description = "description",
      labels = Seq(Label(Some("category"), Seq("interesting"))),
      articleApiId = None,
      coverPhotoUrl = None
    )
    val domainLabel = domain.Label(Some("category"), Seq("interesting"))
    val expectedResult = Cover(
      Some(1),
      Some(1),
      Some(10001),
      sampleCover.coverPhotoUrl,
      sampleCover.title ++ Seq(Title(toUpdate.title, toUpdate.language)),
      sampleCover.description ++ Seq(Description(toUpdate.description, toUpdate.language)),
      sampleCover.labels ++ Seq(LanguageLabels(Seq(domainLabel), toUpdate.language)),
      sampleCover.articleApiId,
      sampleCover.updatedBy,
      sampleCover.updated,
      sampleCover.theme
    )

    when(converterService.toDomainLabel(any[Label])).thenReturn(domainLabel)
    service.mergeCovers(sampleCover, toUpdate) should equal(expectedResult)
  }

  test("mergeCovers overwrite a langauge if specified language already exist in cover") {
    val toUpdate = sampleApiUpdateCover.copy(
      language = "nb",
      title = "titl",
      description = "description",
      labels = Seq(Label(Some("category"), Seq("interesting"))),
      articleApiId = None,
      coverPhotoUrl = None
    )
    val domainLabel = domain.Label(Some("category"), Seq("interesting"))
    val expectedResult = Cover(
      Some(1),
      Some(1),
      Some(10001),
      sampleCover.coverPhotoUrl,
      Seq(Title(toUpdate.title, toUpdate.language)),
      Seq(Description(toUpdate.description, toUpdate.language)),
      Seq(LanguageLabels(Seq(domainLabel), toUpdate.language)),
      sampleCover.articleApiId,
      sampleCover.updatedBy,
      sampleCover.updated,
      sampleCover.theme
    )

    when(converterService.toDomainLabel(any[Label])).thenReturn(domainLabel)
    service.mergeCovers(sampleCover, toUpdate) should equal(expectedResult)
  }

  test("mergeCovers updates optional values if specified") {
    val toUpdate = sampleApiUpdateCover.copy(
      language = "nb",
      title = "titl",
      description = "description",
      labels = Seq(Label(Some("category"), Seq("interesting"))),
      articleApiId = Some(987),
      coverPhotoUrl = Some("https://updated-image.jpg")
    )
    val domainLabel = domain.Label(Some("category"), Seq("interesting"))
    val expectedResult = Cover(
      Some(1),
      Some(1),
      Some(10001),
      toUpdate.coverPhotoUrl.get,
      Seq(Title(toUpdate.title, toUpdate.language)),
      Seq(Description(toUpdate.description, toUpdate.language)),
      Seq(LanguageLabels(Seq(domainLabel), toUpdate.language)),
      toUpdate.articleApiId.get,
      "content-import-client",
      sampleCover.updated,
      sampleCover.theme
    )

    when(converterService.toDomainLabel(any[Label])).thenReturn(domainLabel)
    service.mergeCovers(sampleCover, toUpdate) should equal(expectedResult)
  }

  test("mergeCovers should always use toMerge's revision number") {
    val toUpdate = sampleApiUpdateCover.copy(
      language = "nb",
      revision = 1000,
      title = "titl",
      description = "description",
      labels = Seq(Label(Some("category"), Seq("interesting"))),
      articleApiId = Some(987),
      coverPhotoUrl = Some("https://updated-image.jpg")
    )
    val domainLabel = domain.Label(Some("category"), Seq("interesting"))
    val expectedResult = Cover(
      Some(1),
      Some(1000),
      Some(10001),
      toUpdate.coverPhotoUrl.get,
      Seq(Title(toUpdate.title, toUpdate.language)),
      Seq(Description(toUpdate.description, toUpdate.language)),
      Seq(LanguageLabels(Seq(domainLabel), toUpdate.language)),
      toUpdate.articleApiId.get,
      "content-import-client",
      sampleCover.updated,
      sampleCover.theme
    )

    when(converterService.toDomainLabel(any[Label])).thenReturn(domainLabel)
    service.mergeCovers(sampleCover, toUpdate) should equal(expectedResult)
  }

}
