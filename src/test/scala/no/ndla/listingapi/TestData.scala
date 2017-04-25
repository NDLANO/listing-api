package no.ndla.listingapi

import no.ndla.listingapi.model.domain.{Label, LanguageLabels}
import no.ndla.listingapi.model.{api, domain}

object TestData {

  val sampleCover = domain.Cover(
    Some(1),
    Some(1),
    "https://test.api.ndla.no/image-api/v1/raw/image.jpg",
    Seq(domain.Title("hammer", Some("nb"))),
    Seq(domain.Description("En hammer er et nyttig verktøy", Some("nb"))),
    Seq(LanguageLabels(Seq(Label(Some("kategori"), Seq("personlig verktøy")), Label(None, Seq("bygg"))), Some("nb"))),
    1122,
    "NDLA import script"
  )

  val sampleApiCover = api.Cover(
    1,
    1,
    "https://i.ndla.no/image-api/v1/raw/image.jpg",
    "hammer",
    "En hammer er et nyttig verktøy",
    1122,
    Seq(api.Label(Some("kategori"), Seq("personlig verktøy")), api.Label(None, Seq("bygg"))),
    Seq("nb"),
    "NDLA import script"
  )

  val sampleApiNewCover = api.NewCover(
    language = "nb",
    "https://i.ndla.no/image-api/v1/raw/image.jpg",
    "hammer",
    "En hammer er et nyttig verktøy",
    1122,
    Seq(api.Label(Some("kategori"), Seq("personlig verktøy")), api.Label(None, Seq("bygg")))
  )

  val sampleApiUpdateCover = api.UpdateCover(
    "nb",
    1,
    None,
    None,
    "hammer",
    "En hammer er et nyttig verktøy",
    Seq(api.Label(Some("kategori"), Seq("personlig verktøy")), api.Label(None, Seq("bygg")))
  )

}
