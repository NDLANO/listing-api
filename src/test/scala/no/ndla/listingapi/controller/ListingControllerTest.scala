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

  override val authClient = new AuthClient

  lazy val henrik = new ListingController
  addServlet(henrik, "/test")

  val legacyAuthHeaderWithWriteRole = "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhcHBfbWV0YWRhdGEiOnsicm9sZXMiOlsibGlzdGluZzp3cml0ZSJdLCJuZGxhX2lkIjoiY29udGVudC1pbXBvcnQtY2xpZW50In0sIm5hbWUiOiJEb25hbGQgRHVjayIsImlzcyI6Imh0dHBzOi8vc29tZS1kb21haW4vIiwic3ViIjoiZ29vZ2xlLW9hdXRoMnwxMjMiLCJhdWQiOiJhYmMiLCJleHAiOjE0ODYwNzAwNjMsImlhdCI6MTQ4NjAzNDA2M30.Q3_CVE9WsfhSkc7rgIi6jwbom9PKYpyJRY3PVa1f7uU"
  val legacyAuthHeaderWithoutAnyRoles = "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhcHBfbWV0YWRhdGEiOnsicm9sZXMiOltdLCJuZGxhX2lkIjoiY29udGVudC1pbXBvcnQtY2xpZW50In0sIm5hbWUiOiJEb25hbGQgRHVjayIsImlzcyI6Imh0dHBzOi8vc29tZS1kb21haW4vIiwic3ViIjoiZ29vZ2xlLW9hdXRoMnwxMjMiLCJhdWQiOiJhYmMiLCJleHAiOjE0ODYwNzAwNjMsImlhdCI6MTQ4NjAzNDA2M30.znoUoNnpckY-jeflE8263hri-Yl1Mwf6Uz8cO3-wwaE"
  val legacyAuthHeaderWithEmptyNdlaId = "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhcHBfbWV0YWRhdGEiOnsicm9sZXMiOltdLCJuZGxhX2lkIjoiIn0sIm5hbWUiOiJEb25hbGQgRHVjayIsImlzcyI6Imh0dHBzOi8vc29tZS1kb21haW4vIiwic3ViIjoiZ29vZ2xlLW9hdXRoMnwxMjMiLCJhdWQiOiJhYmMiLCJleHAiOjE0ODYwNzAwNjMsImlhdCI6MTQ4NjAzNDA2M30.m5WUK_EJQoUaGDLhE_0g70BMahY0EFhgKJAg420nbnw"
  val legacyAuthHeaderWithWrongRole = "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhcHBfbWV0YWRhdGEiOnsicm9sZXMiOlsibGlzdGluZzpyZWFkIl0sIm5kbGFfaWQiOiJjb250ZW50LWltcG9ydC1jbGllbnQifSwibmFtZSI6IkRvbmFsZCBEdWNrIiwiaXNzIjoiaHR0cHM6Ly9zb21lLWRvbWFpbi8iLCJzdWIiOiJnb29nbGUtb2F1dGgyfDEyMyIsImF1ZCI6ImFiYyIsImV4cCI6MTQ4NjA3MDA2MywiaWF0IjoxNDg2MDM0MDYzfQ.nDrdxmP9AhlqX_gNIJOIu4_yQP-fRnICH4Mk4PcFVok"

  val authHeaderWithWriteRole = "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjoieHh4eXl5IiwiaXNzIjoiaHR0cHM6Ly9uZGxhLmV1LmF1dGgwLmNvbS8iLCJzdWIiOiJ4eHh5eXlAY2xpZW50cyIsImF1ZCI6Im5kbGFfc3lzdGVtIiwiaWF0IjoxNTEwMzA1NzczLCJleHAiOjE1MTAzOTIxNzMsInNjb3BlIjoibGlzdGluZzp3cml0ZSIsImd0eSI6ImNsaWVudC1jcmVkZW50aWFscyJ9.kPdKx3VVtgjI4vDi1fiM28FFf7yAGV0d-ZxWf5JT-QtQNsTshfp7eh5JMxnmJj8Fkkih-GJHrFdvthhOt_RU7uWc-3xzhJbjWYw_4QIzjYM6Igx3DpvqEYGznbylLXoCiUt5G4bEBvbb0RqvJ9QtA_LmMtmuNu-CCgNvVjCBrxo"
  val authHeaderWithoutAnyRoles = "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjoieHh4eXl5IiwiaXNzIjoiaHR0cHM6Ly9uZGxhLmV1LmF1dGgwLmNvbS8iLCJzdWIiOiJ4eHh5eXlAY2xpZW50cyIsImF1ZCI6Im5kbGFfc3lzdGVtIiwiaWF0IjoxNTEwMzA1NzczLCJleHAiOjE1MTAzOTIxNzMsInNjb3BlIjoiIiwiZ3R5IjoiY2xpZW50LWNyZWRlbnRpYWxzIn0.fb9eTuBwIlbGDgDKBQ5FVpuSUdgDVBZjCenkOrWLzUByVCcaFhbFU8CVTWWKhKJqt6u-09-99hh86szURLqwl3F5rxSX9PrnbyhI9LsPut_3fr6vezs6592jPJRbdBz3-xLN0XY5HIiJElJD3Wb52obTqJCrMAKLZ5x_GLKGhcY"
  val authHeaderWithEmptyClientId = "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjoiIiwiaXNzIjoiaHR0cHM6Ly9uZGxhLmV1LmF1dGgwLmNvbS8iLCJzdWIiOiJ4eHh5eXlAY2xpZW50cyIsImF1ZCI6Im5kbGFfc3lzdGVtIiwiaWF0IjoxNTEwMzA1NzczLCJleHAiOjE1MTAzOTIxNzMsInNjb3BlIjoibGlzdGluZzp3cml0ZSIsImd0eSI6ImNsaWVudC1jcmVkZW50aWFscyJ9.BsgFgHkdKIxH1Ew7_kQAF3y7dBHEdH_cdlpGHDamp1sj2ixns-bCOknlo2e4-ZpxZttgV2IhURr0l3MyPLMhAE_gZNagPORRB8zdBnnet_GrNNlqw01Gd_Vj34hPfB8GQ53sE36CniCetIqqI31ahUw2KH3MT82Y6sLu7cnMGRY"
  val authHeaderWithWrongRole = "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ik9FSTFNVVU0T0RrNU56TTVNekkyTXpaRE9EazFOMFl3UXpkRE1EUXlPRFZDUXpRM1FUSTBNQSJ9.eyJodHRwczovL25kbGEubm8vY2xpZW50X2lkIjoieHh4eXl5IiwiaXNzIjoiaHR0cHM6Ly9uZGxhLmV1LmF1dGgwLmNvbS8iLCJzdWIiOiJ4eHh5eXlAY2xpZW50cyIsImF1ZCI6Im5kbGFfc3lzdGVtIiwiaWF0IjoxNTEwMzA1NzczLCJleHAiOjE1MTAzOTIxNzMsInNjb3BlIjoiYXJ0aWNsZXM6d3JpdGUiLCJndHkiOiJjbGllbnQtY3JlZGVudGlhbHMifQ.kh82qM84FZgoo3odWbHTLWy-N049m7SyQw4gdatDMk43H2nWHA6gjsbJoiBIZ7BcbSfHElEZH0tP94vRy-kjgA3hflhOBbsD73DIxRvnbH1kSXlBnl6ISbgtHnzv1wQ7ShykMAcBsoWQ6J16ixK_p-msW42kcEqK1LanzPy-_qI"

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


  test("/:coverid should return 200 if the cover was found") {
    when(readService.coverWithId(coverId, lang)).thenReturn(Some(TestData.sampleApiCover))

    get(s"/test/$coverId?language=$lang") {
      status should equal (200)
    }
  }

  test("/:coverid should return 404 if the cover was not found") {
    when(readService.coverWithId(coverId, lang)).thenReturn(None)

    get(s"/test/$coverId?language=$lang") {
      status should equal (404)
    }
  }

  test("/:coverid should return 400 if the cover_id is not an integer") {
    get(s"/test/one") {
      status should equal(400)
    }
  }

  test("That POST / returns 403 if no auth-header") {
    post("/test/") {
      status should equal (403)
    }
  }

  test("That PUT /:coverid returns 403 if no auth-header") {
    put("/test/1") {
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
      status should equal (400)
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

  test("PUT /:coverid should return 400 on failure to validate request") {
    put("/test/1", "{}", headers = Map("Authorization" -> authHeaderWithWriteRole)) {
      status should equal(400)
    }
  }

  test("PUT /:coverid should return 200 on success") {
    when(writeService.updateCover(any[Long], any[UpdateCover])).thenReturn(Success(TestData.sampleApiCover))
    put("/test/1", requestBody, headers = Map("Authorization" -> authHeaderWithWriteRole)) {
      status should equal(200)
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

  test("That POST / returns 403 if auth header does not have valid client_id") {
    post("/test/", headers = Map("Authorization" -> authHeaderWithEmptyClientId)) {
      status should equal (403)
    }
  }

  test("That PUT /:coverid returns 403 if auth header does not have valid client_id") {
    put("/test/1", headers = Map("Authorization" -> authHeaderWithEmptyClientId)) {
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

  // Legacy tests. May be removed when the legacy token format in ndla.network v0.24 is removed
  test("LEGACY - POST / should return 400 on failure to validate request") {
    post("/test/", "{}", headers = Map("Authorization" -> legacyAuthHeaderWithWriteRole)) {
      status should equal(400)
    }
  }

  test("LEGACY - POST / should return 200 on success") {
    when(writeService.newCover(any[NewCover])).thenReturn(Success(TestData.sampleApiCover))
    post("/test/", requestBody, headers = Map("Authorization" -> legacyAuthHeaderWithWriteRole)) {
      status should equal(200)
    }
  }

  test("LEGACY - PUT /:coverid should return 400 on failure to validate request") {
    put("/test/1", "{}", headers = Map("Authorization" -> legacyAuthHeaderWithWriteRole)) {
      status should equal(400)
    }
  }

  test("LEGACY - PUT /:coverid should return 200 on success") {
    when(writeService.updateCover(any[Long], any[UpdateCover])).thenReturn(Success(TestData.sampleApiCover))
    put("/test/1", requestBody, headers = Map("Authorization" -> legacyAuthHeaderWithWriteRole)) {
      status should equal(200)
    }
  }

  test("LEGACY - That POST / returns 403 if auth header does not have expected role") {
    post("/test/", headers = Map("Authorization" -> legacyAuthHeaderWithWrongRole)) {
      status should equal (403)
    }
  }

  test("LEGACY - That POST / returns 403 if auth header does not have any roles") {
    post("/test/", headers = Map("Authorization" -> legacyAuthHeaderWithoutAnyRoles)) {
      status should equal (403)
    }
  }

  test("LEGACY - That POST / returns 403 if auth header does not have valid client_id") {
    post("/test/", headers = Map("Authorization" -> legacyAuthHeaderWithEmptyNdlaId)) {
      status should equal (403)
    }
  }

  test("LEGACY - That PUT /:coverid returns 403 if auth header does not have valid client_id") {
    put("/test/1", headers = Map("Authorization" -> legacyAuthHeaderWithEmptyNdlaId)) {
      status should equal (403)
    }
  }

  test("LEGACY - That PUT /:coverid returns 403 if auth header does not have expected role") {
    put("/test/1", headers = Map("Authorization" -> legacyAuthHeaderWithWrongRole)) {
      status should equal (403)
    }
  }

  test("LEGACY - That PUT /:coverid returns 403 if auth header does not have any roles") {
    put("/test/1", headers = Map("Authorization" -> legacyAuthHeaderWithoutAnyRoles)) {
      status should equal (403)
    }
  }
}
