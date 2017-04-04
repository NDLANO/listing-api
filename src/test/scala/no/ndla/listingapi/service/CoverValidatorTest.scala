package no.ndla.listingapi.service

import no.ndla.listingapi.model.domain._
import no.ndla.listingapi.{TestData, TestEnvironment, UnitSuite}

class CoverValidatorTest extends UnitSuite with TestEnvironment {

  val service = new CoverValidator

  val sampleCover = TestData.sampleCover

  test("validate returns a failure if title contains html or an unknown language") {
    service.validate(sampleCover.copy(title=Seq(Title("<h1>title</h1>", Some("nb"))))).isFailure should be(true)
    service.validate(sampleCover.copy(title=Seq(Title("Ash nazg durbatulûk", Some("black speech"))))).isFailure should be(true)
  }

  test("validate returns a failure if description contains html or an unknown language") {
    service.validate(sampleCover.copy(description=Seq(Description("<h1>title</h1>", Some("nb"))))).isFailure should be(true)
    service.validate(sampleCover.copy(description=Seq(Description("Ash nazg durbatulûk", Some("black speech"))))).isFailure should be(true)
  }

  test("validate returns a failure if a label contains html or an unknown language") {
    val labelsWithHtml = LanguageLabels(Seq(Label(None, Seq("<h1>label</h1>"))), Some("nb"))
    val labelsWithHtml2 = LanguageLabels(Seq(Label(Some("<h1>category</h1>"), Seq("label"))), Some("nb"))
    val labelsWithInvalidLang = LanguageLabels(Seq(Label(None, Seq("ash", "nazg"))), Some("black speech"))

    service.validate(sampleCover.copy(labels=Seq(labelsWithHtml))).isFailure should be(true)
    service.validate(sampleCover.copy(labels=Seq(labelsWithHtml2))).isFailure should be(true)
    service.validate(sampleCover.copy(labels=Seq(labelsWithInvalidLang))).isFailure should be(true)
  }

  test("validate returns a failure if a coverPhoto is a link to a non-ndla domain") {
    service.validate(sampleCover.copy(coverPhotoUrl="http://nonndla.org/198273918237.jpg")).isFailure should be(true)
  }

  test("validate returns a failure if a id is less than zero") {
    service.validate(sampleCover.copy(id=Some(-1))).isFailure should be(true)
  }

  test("validate returns a failure if articleApid is less than zero") {
    service.validate(sampleCover.copy(articleApiId = -1)).isFailure should be(true)
  }

  test("validate returns success if cover is valid") {
    service.validate(sampleCover).isSuccess should be (true)
  }

}
