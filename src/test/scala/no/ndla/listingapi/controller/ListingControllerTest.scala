package no.ndla.listingapi.controller

import no.ndla.listingapi.model.api.{NewCover, UpdateCover}
import no.ndla.listingapi.model.domain.UniqeLabels
import no.ndla.listingapi.{ListingApiProperties, ListingSwagger, TestData, TestEnvironment, UnitSuite}
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import org.scalatra.test.scalatest.ScalatraFunSuite

import scala.util.Success

class ListingControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {
  implicit val formats = org.json4s.DefaultFormats
  implicit val swagger = new ListingSwagger

  override val authClient = new AuthClient

  lazy val henrik = new ListingController
  addServlet(henrik, "/test")

  val authHeaderWithWriteRole =
    "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IlJTMjU2In0.eyJhenAiOiJ4eHh5eXkiLCJpc3MiOiJodHRwczovL25kbGEuZXUuYXV0aDAuY29tLyIsInN1YiI6Inh4eHl5eUBjbGllbnRzIiwiYXVkIjoibmRsYV9zeXN0ZW0iLCJpYXQiOjE1MTAzMDU3NzMsImV4cCI6MTUxMDM5MjE3Mywic2NvcGUiOiJsaXN0aW5nOndyaXRlIiwiZ3R5IjoiY2xpZW50LWNyZWRlbnRpYWxzIn0.B1MeD4OCVqZaqLa9GvGjUXhY5XOVnslNRY9dj6mczlk"

  val authHeaderWithoutAnyRoles =
    "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjoieHh4eXl5IiwiaXNzIjoiaHR0cHM6Ly9uZGxhLmV1LmF1dGgwLmNvbS8iLCJzdWIiOiJ4eHh5eXlAY2xpZW50cyIsImF1ZCI6Im5kbGFfc3lzdGVtIiwiaWF0IjoxNTEwMzA1NzczLCJleHAiOjE1MTAzOTIxNzMsInNjb3BlIjoiIiwiZ3R5IjoiY2xpZW50LWNyZWRlbnRpYWxzIn0.fb9eTuBwIlbGDgDKBQ5FVpuSUdgDVBZjCenkOrWLzUByVCcaFhbFU8CVTWWKhKJqt6u-09-99hh86szURLqwl3F5rxSX9PrnbyhI9LsPut_3fr6vezs6592jPJRbdBz3-xLN0XY5HIiJElJD3Wb52obTqJCrMAKLZ5x_GLKGhcY"
//  "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IlJTMjU2In0.eyJhenAiOiJ4eHh5eXkiLCJpc3MiOiJodHRwczovL25kbGEuZXUuYXV0aDAuY29tLyIsInN1YiI6Inh4eHl5eUBjbGllbnRzIiwiYXVkIjoibmRsYV9zeXN0ZW0iLCJpYXQiOjE1MTAzMDU3NzMsImV4cCI6MTUxMDM5MjE3Mywic2NvcGUiOiIiLCJndHkiOiJjbGllbnQtY3JlZGVudGlhbHMifQ.S8ftMt9fObXJPUaOhSKBviXWDxsK5baM44nyIhfA2mQ"

  val authHeaderWithEmptyClientId =
    "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IlJTMjU2In0.eyJhenAiOiIiLCJpc3MiOiJodHRwczovL25kbGEuZXUuYXV0aDAuY29tLyIsInN1YiI6Inh4eHl5eUBjbGllbnRzIiwiYXVkIjoibmRsYV9zeXN0ZW0iLCJpYXQiOjE1MTAzMDU3NzMsImV4cCI6MTUxMDM5MjE3Mywic2NvcGUiOiIiLCJndHkiOiJjbGllbnQtY3JlZGVudGlhbHMifQ.7UvH7xfGDdwTSIhWL2B0eV6-3kMOLUAH-kOJE9cXofo"

  val authHeaderWithWrongRole =
    "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IlJTMjU2In0.eyJhenAiOiJ4eHh5eXkiLCJpc3MiOiJodHRwczovL25kbGEuZXUuYXV0aDAuY29tLyIsInN1YiI6Inh4eHl5eUBjbGllbnRzIiwiYXVkIjoibmRsYV9zeXN0ZW0iLCJpYXQiOjE1MTAzMDU3NzMsImV4cCI6MTUxMDM5MjE3Mywic2NvcGUiOiJkcmFmdHM6d3JpdGUiLCJndHkiOiJjbGllbnQtY3JlZGVudGlhbHMifQ.mY9ZU6einZgtqT719nuT6KTwdCA3ZrsMWyx2plGE4a8"

  val lang = "nb"
  val coverId = 123

  val requestBody =
    """
               |{
               |    "language": "en",
               |    "revision": 1,
               |    "title": "an english title",
               |    "labels": [
               |          {"type": "category", "labels": ["hurr", "durr", "i'm", "not", "english"]}
               |    ],
               |    "articleApiId": 1234,
               |    "description": "dogs and cats",
               |    "coverPhotoUrl": "https://image.imgs/catdog.jpg",
               |    "theme": "verktoy"
               |}
             """.stripMargin

  test("/:coverid should return 200 if the cover was found") {
    when(readService.coverWithId(coverId, lang))
      .thenReturn(Some(TestData.sampleApiCover))

    get(s"/test/$coverId?language=$lang") {
      status should equal(200)
    }
  }

  test("/:coverid should return 404 if the cover was not found") {
    when(readService.coverWithId(coverId, lang)).thenReturn(None)

    get(s"/test/$coverId?language=$lang") {
      status should equal(404)
    }
  }

  test("/:coverid should return 400 if the cover_id is not an integer") {
    get(s"/test/one") {
      status should equal(400)
    }
  }

  test("That POST / returns 403 if no auth-header") {
    post("/test/") {
      status should equal(403)
    }
  }

  test("That PUT /:coverid returns 403 if no auth-header") {
    put("/test/1") {
      status should equal(403)
    }
  }

  test("That GET /labels/ returns 200 and map of all uniqe labels") {
    when(readService.allLabelsMap()).thenReturn(Map("x" -> UniqeLabels(Map())))
    get("/test/labels/") {
      status should equal(200)
    }
  }

  test("That GET /labels/subjects returns 200 and map of all uniqe labels") {
    get("/test/labels/subjects") {
      status should equal(200)
    }
  }

  test("That GET /theme/:theme returns 200 and sequence of covers of that theme") {
    get("/test/theme/verktoy") {
      status should equal(200)
    }
  }

  test("That GET /theme/:theme returns 400 on non valid theme") {
    get("/test/theme/notValid") {
      status should equal(400)
    }
  }

  test("POST / should return 400 on failure to validate request") {
    post("/test/", "{}", headers = Map("Authorization" -> authHeaderWithWriteRole)) {
      status should equal(400)
    }
  }

  test("POST / should return 200 on success") {
    when(writeService.newCover(any[NewCover]))
      .thenReturn(Success(TestData.sampleApiCover))
    post("/test/", requestBody, headers = Map("Authorization" -> authHeaderWithWriteRole)) {
      status should equal(200)
    }
  }

  test("PUT /:coverid should return 400 on failure to validate request") {
    put("/test/1", "{}", headers = Map("Authorization" -> authHeaderWithWriteRole)) {
      status should equal(400)
    }
  }

  test("PUT /:coverid should return 200 on success") {
    when(writeService.updateCover(any[Long], any[UpdateCover]))
      .thenReturn(Success(TestData.sampleApiCover))
    put("/test/1", requestBody, headers = Map("Authorization" -> authHeaderWithWriteRole)) {
      status should equal(200)
    }
  }

  test("That POST / returns 403 if auth header does not have expected role") {
    post("/test/", headers = Map("Authorization" -> authHeaderWithWrongRole)) {
      status should equal(403)
    }
  }

  test("That POST / returns 403 if auth header does not have any roles") {
    post("/test/", headers = Map("Authorization" -> authHeaderWithoutAnyRoles)) {
      status should equal(403)
    }
  }

  test("That POST / returns 403 if auth header does not have valid client_id") {
    post("/test/", headers = Map("Authorization" -> authHeaderWithEmptyClientId)) {
      status should equal(403)
    }
  }

  test("That PUT /:coverid returns 403 if auth header does not have valid client_id") {
    put("/test/1", headers = Map("Authorization" -> authHeaderWithEmptyClientId)) {
      status should equal(403)
    }
  }

  test("That PUT /:coverid returns 403 if auth header does not have expected role") {
    put("/test/1", headers = Map("Authorization" -> authHeaderWithWrongRole)) {
      status should equal(403)
    }
  }

  test("That PUT /:coverid returns 403 if auth header does not have any roles") {
    put("/test/1", headers = Map("Authorization" -> authHeaderWithoutAnyRoles)) {
      status should equal(403)
    }
  }
}
