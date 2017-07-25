package no.ndla.listingapi.controller

import no.ndla.listingapi.model.api.{NewCover, UpdateCover}
import no.ndla.listingapi.model.domain.UniqeLabels
import no.ndla.listingapi.{ListingApiProperties, ListingSwagger, TestData, TestEnvironment, UnitSuite}
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.scalatra.test.scalatest.ScalatraFunSuite

import scala.util.Success


class ListingControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {
  implicit val formats = org.json4s.DefaultFormats
  implicit val swagger = new ListingSwagger

  lazy val henrik = new ListingController
  addServlet(henrik, "/test")

  val jwtHeader = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9"

  val jwtClaims = "eyJhcHBfbWV0YWRhdGEiOnsicm9sZXMiOlsibGlzdGluZzp3cml0ZSJdLCJuZGxhX2lkIjoiYWJjMTIzIn0sIm5hbWUiOiJEb25hbGQgRHVjayIsImlzcyI6Imh0dHBzOi8vc29tZS1kb21haW4vIiwic3ViIjoiZ29vZ2xlLW9hdXRoMnwxMjMiLCJhdWQiOiJhYmMiLCJleHAiOjE0ODYwNzAwNjMsImlhdCI6MTQ4NjAzNDA2M30"
  val jwtClaimsNoRoles = "eyJhcHBfbWV0YWRhdGEiOnsicm9sZXMiOltdLCJuZGxhX2lkIjoiYWJjMTIzIn0sIm5hbWUiOiJEb25hbGQgRHVjayIsImlzcyI6Imh0dHBzOi8vc29tZS1kb21haW4vIiwic3ViIjoiZ29vZ2xlLW9hdXRoMnwxMjMiLCJhdWQiOiJhYmMiLCJleHAiOjE0ODYwNzAwNjMsImlhdCI6MTQ4NjAzNDA2M30"
  val jwtClaimsWrongRole = "eyJhcHBfbWV0YWRhdGEiOnsicm9sZXMiOlsibGlzdGluZzpyZWFkIl0sIm5kbGFfaWQiOiJhYmMxMjMifSwibmFtZSI6IkRvbmFsZCBEdWNrIiwiaXNzIjoiaHR0cHM6Ly9zb21lLWRvbWFpbi8iLCJzdWIiOiJnb29nbGUtb2F1dGgyfDEyMyIsImF1ZCI6ImFiYyIsImV4cCI6MTQ4NjA3MDA2MywiaWF0IjoxNDg2MDM0MDYzfQ"
  val jwtClaimsEmptyNdlaId = "eyJhcHBfbWV0YWRhdGEiOnsicm9sZXMiOltdLCJuZGxhX2lkIjoiIn0sIm5hbWUiOiJEb25hbGQgRHVjayIsImlzcyI6Imh0dHBzOi8vc29tZS1kb21haW4vIiwic3ViIjoiZ29vZ2xlLW9hdXRoMnwxMjMiLCJhdWQiOiJhYmMiLCJleHAiOjE0ODYwNzAwNjMsImlhdCI6MTQ4NjAzNDA2M30"

  val authHeaderWithWriteRole = s"Bearer $jwtHeader.$jwtClaims.5_LMfZxYsyj-8iUKQsy-pZMOUD56jUIMMf6qeZFjdOc"
  val authHeaderWithoutAnyRoles = s"Bearer $jwtHeader.$jwtClaimsNoRoles.3iwx0qpBiYGGCUbOyGjEBWM3MoxdJm9hFSlReEHc2cM"
  val authHeaderWithEmptyNdlaId = s"Bearer $jwtHeader.$jwtClaimsEmptyNdlaId.m5WUK_EJQoUaGDLhE_0g70BMahY0EFhgKJAg420nbnw"
  val authHeaderWithWrongRole = s"Bearer $jwtHeader.$jwtClaimsWrongRole.DJBSNf0KYxTy3kqPAmWET8TU0awSqscAaPk0RgEiyvo"

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
               |    "coverPhotoUrl": "https://image.imgs/catdog.jpg",
               |    "theme": "verktoy"
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
    post("/test/", "{}", headers = Map("Authorization" -> authHeaderWithWriteRole)) {
      status should equal(400)
    }
  }

  test("POST / should return 200 on success") {
    when(writeService.newCover(any[NewCover])).thenReturn(Success(TestData.sampleApiCover))
    post("/test/", requestBody, headers = Map("Authorization" -> authHeaderWithWriteRole)) {
      status should equal(200)
    }
  }

  test("PUT /:cover-id should return 400 on failure to validate request") {
    put("/test/1", "{}", headers = Map("Authorization" -> authHeaderWithWriteRole)) {
      status should equal(400)
    }
  }

  test("PUT /:cover-id should return 200 on success") {
    when(writeService.updateCover(any[Long], any[UpdateCover])).thenReturn(Success(TestData.sampleApiCover))
    put("/test/1", requestBody, headers = Map("Authorization" -> authHeaderWithWriteRole)) {
      status should equal(200)
    }
  }

  test("That POST / returns 403 if no auth-header") {
    post("/test/") {
      status should equal (403)
    }
  }

  test("That POST / returns 403 if auth header does not have expected role") {
    post("/test/", headers = Map("Authorization" -> authHeaderWithWrongRole)) {
      status should equal (403)
    }
  }

  test("That POST / returns 403 if auth header does not have any roles") {
    post("/test/", headers = Map("Authorization" -> authHeaderWithoutAnyRoles)) {
      status should equal (403)
    }
  }

  test("That POST / returns 403 if auth header does not have valid ndla_id") {
    post("/test/", headers = Map("Authorization" -> authHeaderWithEmptyNdlaId)) {
      status should equal (403)
    }
  }

  test("That PUT /:coverid returns 403 if no auth-header") {
    put("/test/1") {
      status should equal (403)
    }
  }

  test("That PUT /:cover_id returns 403 if auth header does not have valid ndla_id") {
    put("/test/1", headers = Map("Authorization" -> authHeaderWithEmptyNdlaId)) {
      status should equal (403)
    }
  }

  test("That PUT /:coverid returns 403 if auth header does not have expected role") {
    put("/test/1", headers = Map("Authorization" -> authHeaderWithWrongRole)) {
      status should equal (403)
    }
  }

  test("That PUT /:coverid returns 403 if auth header does not have any roles") {
    put("/test/1", headers = Map("Authorization" -> authHeaderWithoutAnyRoles)) {
      status should equal (403)
    }
  }

  test("That GET /labels/ returns 200 and map of all uniqe labels"){
    when(readService.allLabelsMap()).thenReturn(Map("x" -> UniqeLabels(Map())))
    get("/test/labels/") {
      status should equal (200)
    }
  }

  test("That GET /labels/subjects returns 200 and map of all uniqe labels"){
    get("/test/labels/subjects") {
      status should equal (200)
    }
  }


  test("That GET /theme/:theme returns 200 and sequence of covers of that theme"){
    get("/test/theme/verktoy") {
      status should equal (200)
    }
  }

  test("That GET /theme/:theme returns 400 on non valid theme"){
    get("/test/theme/notValid") {
      status should equal (404)
    }
  }

}
