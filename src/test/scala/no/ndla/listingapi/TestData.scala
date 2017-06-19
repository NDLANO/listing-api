package no.ndla.listingapi

import no.ndla.listingapi.model.domain.{Label, LanguageLabels}
import no.ndla.listingapi.model.{api, domain}
import org.joda.time.{DateTime, DateTimeZone}

object TestData {

  val sampleCover = domain.Cover(
    Some(1),
    Some(1),
    None,
    "https://test.api.ndla.no/image-api/v1/raw/image.jpg",
    Seq(domain.Title("hammer", Some("nb"))),
    Seq(domain.Description("En hammer er et nyttig verktøy", Some("nb"))),
    Seq(LanguageLabels(Seq(Label(Some("kategori"), Seq("personlig verktøy", "bygg verktøy")), Label(None, Seq("bygg"))), Some("nb"))),
    1122,
    "NDLA import script",
    updated
  )

  val sampleCover2 = domain.Cover(
    Some(1),
    Some(1),
    None,
    "https://test.api.ndla.no/image-api/v1/raw/image.jpg",
    Seq(domain.Title("hammer2", Some("nb"))),
    Seq(domain.Description("En hammer2 er et nyttig verktøy", Some("nb"))),
    Seq(
      LanguageLabels(
        Seq(Label(Some("kategori"), Seq("jobbe verktøy", "mer label")), Label(None, Seq("byggherrer"))),
        Some("nb")),
      LanguageLabels(
        Seq(Label(Some("kategori"), Seq("arbeids verktøy")), Label(None, Seq("byggkarer"))),
        Some("nn")),
      LanguageLabels(
        Seq(Label(Some("category"), Seq("work tools")), Label(None, Seq("workmen"))),
        Some("en"))
    ),
    1122,
    "NDLA import script",
    updated
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
    "NDLA import script",
    updated
  )

  val sampleApiNewCover = api.NewCover(
    language = "nb",
    "https://i.ndla.no/image-api/v1/raw/image.jpg",
    "hammer",
    "En hammer er et nyttig verktøy",
    1122,
    None,
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

  def updated() = (new DateTime(2017, 4, 1, 12, 15, 32, DateTimeZone.UTC)).toDate

}
