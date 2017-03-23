package no.ndla.listingapi

import no.ndla.listingapi.model.domain.Label
import no.ndla.listingapi.model.{domain, api}

object TestData {

  val sampleCover = domain.Cover(
    Some(1),
    "https://image-api/image.jpg",
    "hammer",
    "En hammer er et nyttig verktøy",
    Seq(Label(Some("kategori"), Seq("personlig verktøy")), Label(None, Seq("bygg"))),
    1122
  )

  val sampleApiCover = api.Cover(
    1,
    "https://image-api/image.jpg",
    "hammer",
    "En hammer er et nyttig verktøy",
    1122,
    Seq(api.Label(Some("kategori"), Seq("personlig verktøy")), api.Label(None, Seq("bygg")))
  )

}
