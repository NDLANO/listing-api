package no.ndla.listingapi.service

import no.ndla.listingapi.model.api.{Label, NewCover, ValidationException}
import no.ndla.listingapi.model.domain
import no.ndla.listingapi.model.domain._
import no.ndla.listingapi.{TestData, TestEnvironment, UnitSuite}
import org.mockito.Matchers._
import org.mockito.Mockito._
import scalikejdbc.DBSession

import scala.util.{Failure, Success}

class WriteServiceTest extends UnitSuite with TestEnvironment {
  val service = new WriteService

  val sampleNewCover = TestData.sampleApiNewCover
  val sampleApiCover = TestData.sampleApiCover
  val sampleCover = TestData.sampleCover
  val sampleApiUpdateCover = TestData.sampleApiUpdateCover

  override def beforeEach = {
    reset(listingRepository)
  }

  test("newCover should return Failure if validation fails") {
    when(coverValidator.validate(any[Cover])).thenReturn(Failure(mock[ValidationException]))
    service.newCover(sampleNewCover).isFailure should be (true)
  }

  test("newCover should return Failure on failure to store cover") {
    when(converterService.toDomainCover(any[NewCover])).thenReturn(sampleCover)
    when(coverValidator.validate(any[Cover])).thenReturn(Success(sampleCover))
    when(listingRepository.newCover(any[Cover])(any[DBSession])).thenThrow(new RuntimeException())

    service.newCover(sampleNewCover).isFailure should be (true)
  }

  test("newCover should return Failure on failure to index cover") {
    when(converterService.toDomainCover(any[NewCover])).thenReturn(sampleCover)
    when(coverValidator.validate(any[Cover])).thenReturn(Success(sampleCover))
    when(listingRepository.newCover(any[Cover])(any[DBSession])).thenReturn(sampleCover)
    when(indexService.indexDocument(sampleCover)).thenReturn(Failure(new RuntimeException()))

    service.newCover(sampleNewCover).isFailure should be (true)
  }

  test("newCover should return Success when everything is fine") {
    when(converterService.toDomainCover(any[NewCover])).thenReturn(sampleCover)
    when(coverValidator.validate(any[Cover])).thenReturn(Success(sampleCover))
    when(listingRepository.newCover(any[Cover])(any[DBSession])).thenReturn(sampleCover)
    when(indexService.indexDocument(sampleCover)).thenReturn(Success(sampleCover))
    when(converterService.toApiCover(sampleCover, "nb")).thenReturn(Success(sampleApiCover))

    service.newCover(sampleNewCover).isSuccess should be (true)
  }

  test("updateCover Failure if cover was not found") {
    when(listingRepository.getCover(any[Long])(any[DBSession])).thenReturn(None)
    service.updateCover(1, sampleApiUpdateCover).isFailure should be (true)
  }

  test("updateCover should return Failure if validation fails") {
    when(coverValidator.validate(any[Cover])).thenReturn(Failure(mock[ValidationException]))
    when(listingRepository.getCover(any[Long])(any[DBSession])).thenReturn(Some(sampleCover))
    service.updateCover(1, sampleApiUpdateCover).isFailure should be (true)
  }

  test("updateCover should return Failure on failure to store cover") {
    when(coverValidator.validate(any[Cover])).thenReturn(Failure(mock[ValidationException]))
    when(listingRepository.getCover(any[Long])(any[DBSession])).thenReturn(Some(sampleCover))
    when(listingRepository.updateCover(any[Cover])(any[DBSession])).thenThrow(new RuntimeException)

    service.updateCover(1, sampleApiUpdateCover).isFailure should be (true)
  }

  test("updateCover should return Failure on failure to index cover") {
    when(coverValidator.validate(any[Cover])).thenReturn(Failure(mock[ValidationException]))
    when(listingRepository.getCover(any[Long])(any[DBSession])).thenReturn(Some(sampleCover))
    when(listingRepository.updateCover(any[Cover])(any[DBSession])).thenReturn(Success(sampleCover))
    when(indexService.indexDocument(sampleCover)).thenReturn(Failure(new RuntimeException))

    service.updateCover(1, sampleApiUpdateCover).isFailure should be (true)
  }

  test("updateCover should return Success if everything is fine") {
    when(coverValidator.validate(any[Cover])).thenReturn(Failure(mock[ValidationException]))
    when(listingRepository.getCover(any[Long])(any[DBSession])).thenReturn(Some(sampleCover))
    when(listingRepository.updateCover(any[Cover])(any[DBSession])).thenReturn(Success(sampleCover))
    when(indexService.indexDocument(sampleCover)).thenReturn(Success(sampleCover))

    service.updateCover(1, sampleApiUpdateCover).isFailure should be (true)
  }

  test("mergeCovers should append a new language if language not already exists") {
    val toUpdate = sampleApiUpdateCover.copy(
      language="en",
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
      sampleCover.coverPhotoUrl,
      sampleCover.title ++ Seq(Title(toUpdate.title, Some(toUpdate.language))),
      sampleCover.description ++ Seq(Description(toUpdate.description, Some(toUpdate.language))),
      sampleCover.labels ++ Seq(LanguageLabels(Seq(domainLabel), Some(toUpdate.language))),
      sampleCover.articleApiId
    )

    when(converterService.toDomainLabel(any[Label])).thenReturn(domainLabel)
    service.mergeCovers(sampleCover, toUpdate) should equal(expectedResult)
  }

  test("mergeCovers overwrite a langauge if specified language already exist in cover") {
    val toUpdate = sampleApiUpdateCover.copy(
      language="nb",
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
      sampleCover.coverPhotoUrl,
      Seq(Title(toUpdate.title, Some(toUpdate.language))),
      Seq(Description(toUpdate.description, Some(toUpdate.language))),
      Seq(LanguageLabels(Seq(domainLabel), Some(toUpdate.language))),
      sampleCover.articleApiId
    )

    when(converterService.toDomainLabel(any[Label])).thenReturn(domainLabel)
    service.mergeCovers(sampleCover, toUpdate) should equal(expectedResult)
  }

  test("mergeCovers updates optional values if specified") {
    val toUpdate = sampleApiUpdateCover.copy(
      language="nb",
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
      toUpdate.coverPhotoUrl.get,
      Seq(Title(toUpdate.title, Some(toUpdate.language))),
      Seq(Description(toUpdate.description, Some(toUpdate.language))),
      Seq(LanguageLabels(Seq(domainLabel), Some(toUpdate.language))),
      toUpdate.articleApiId.get
    )

    when(converterService.toDomainLabel(any[Label])).thenReturn(domainLabel)
    service.mergeCovers(sampleCover, toUpdate) should equal(expectedResult)
  }

  test("mergeCovers should always use toMerge's revision number") {
    val toUpdate = sampleApiUpdateCover.copy(
      language="nb",
      revision=1000,
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
      toUpdate.coverPhotoUrl.get,
      Seq(Title(toUpdate.title, Some(toUpdate.language))),
      Seq(Description(toUpdate.description, Some(toUpdate.language))),
      Seq(LanguageLabels(Seq(domainLabel), Some(toUpdate.language))),
      toUpdate.articleApiId.get
    )

    when(converterService.toDomainLabel(any[Label])).thenReturn(domainLabel)
    service.mergeCovers(sampleCover, toUpdate) should equal(expectedResult)
  }

}
