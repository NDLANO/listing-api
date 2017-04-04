package no.ndla.listingapi.controller

import no.ndla.listingapi.model.api.{NewCover, UpdateCover}
import no.ndla.listingapi.{ListingSwagger, TestData, TestEnvironment, UnitSuite}
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.scalatra.test.scalatest.ScalatraFunSuite

import scala.util.Success


class ListingControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {
  implicit val formats = org.json4s.DefaultFormats
  implicit val swagger = new ListingSwagger

  lazy val henrik = new ListingController
  addServlet(henrik, "/test")

  val lang = "nb"
  val coverId = 123

  val requestBody = """
               |{
               |    "language": "en",
               |    "revision": 1,
               |    "title": "an english title",
               |    "labels": [
               |          {"type": "category", "labels": ["hurr", "durr", "i'm", "not", "english"]}
               |    ],
               |    "articleApiId": 1234,
               |    "description": "dogs and cats",
               |    "coverPhotoUrl": "https://image.imgs/catdog.jpg"
               |}
             """.stripMargin

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

  test("POST / should return 400 on failure to validate request") {
    post("/test/", "{}") {
      status should equal(400)
    }
  }

  test("POST / should return 200 on success") {
    when(writeService.newCover(any[NewCover])).thenReturn(Success(TestData.sampleApiCover))
    post("/test/", requestBody) {
      status should equal(200)
    }
  }

  test("PUT /:cover-id should return 400 on failure to validate request") {
    put("/test/1", "{}") {
      status should equal(400)
    }
  }

  test("PUT /:cover-id should return 200 on success") {
    when(writeService.updateCover(any[Long], any[UpdateCover])).thenReturn(Success(TestData.sampleApiCover))
    put("/test/1", requestBody) {
      status should equal(200)
    }
  }

}
