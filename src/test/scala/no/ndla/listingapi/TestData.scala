package no.ndla.listingapi

import no.ndla.listingapi.model.domain.Label
import no.ndla.listingapi.model.domain

object TestData {

  val sampleCard = domain.Card(
    Some(1),
    "https://image-api/image.jpg",
    "hammer",
    "En hammer er et nyttig verktøy",
    Seq(Label(Some("kategori"), Seq("personlig verktøy")), Label(None, Seq("bygg"))),
    1122
  )

}
