package no.ndla.listingapi.controller

import no.ndla.listingapi.{ListingSwagger, TestData, TestEnvironment, UnitSuite}
import org.mockito.Mockito._
import org.scalatra.test.scalatest.ScalatraFunSuite

class ListingControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {
  implicit val formats = org.json4s.DefaultFormats
  implicit val swagger = new ListingSwagger

  lazy val henrik = new ListingController
  addServlet(henrik, "/test")

  val lang = "nb"
  val coverId = 123

  test("/<cover_id> should return 200 if the cover was found") {
    when(readService.coverWithId(coverId, lang)).thenReturn(Some(TestData.sampleApiCover))

    get(s"/test/$coverId?language=$lang") {
      status should equal (200)
    }
  }

  test("/<cover_id> should return 404 if the cover was not found") {
    when(readService.coverWithId(coverId, lang)).thenReturn(None)

    get(s"/test/$coverId?language=$lang") {
      status should equal (404)
    }
  }

  test("/<cover_id> should return 400 if the cover_id is not an integer") {
    get(s"/test/one") {
      status should equal(400)
    }
  }

}
