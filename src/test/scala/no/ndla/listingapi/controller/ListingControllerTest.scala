package no.ndla.listingapi.controller

import no.ndla.listingapi.{ListingSwagger, TestData, TestEnvironment, UnitSuite}
import org.mockito.Mockito._
import org.scalatra.test.scalatest.ScalatraFunSuite

class ListingControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {
  implicit val formats = org.json4s.DefaultFormats
  implicit val swagger = new ListingSwagger

  lazy val henrik = new ListingController
  addServlet(henrik, "/test")

  val cardId = 123

  test("/<card_id> should return 200 if the card was found") {
    when(readService.cardWithId(cardId)).thenReturn(Some(TestData.sampleApiCard))

    get(s"/test/$cardId") {
      status should equal (200)
    }
  }

  test("/<card_id> should return 404 if the card was not found") {
    when(readService.cardWithId(cardId)).thenReturn(None)

    get(s"/test/$cardId") {
      status should equal (404)
    }
  }

  test("/<card_id> should return 400 if the card_id is not an integer") {
    get(s"/test/one") {
      status should equal(400)
    }
  }

}
