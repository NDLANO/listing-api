package no.ndla.listingapi

import no.ndla.listingapi.model.domain.{Label, LanguageLabels}
import no.ndla.listingapi.model.{api, domain}
import org.joda.time.{DateTime, DateTimeZone}

object TestData {
  val updated = new DateTime(2017, 4, 1, 12, 15, 32, DateTimeZone.UTC).toDate

  val sampleCover = domain.Cover(
    Some(1),
    Some(1),
    Some(10001),
    "https://test.api.ndla.no/image-api/v1/raw/image.jpg",
    Seq(domain.Title("hammer", "nb")),
    Seq(domain.Description("En hammer er et nyttig verktøy", "nb")),
    Seq(
      LanguageLabels(Seq(Label(Some("kategori"), Seq("personlig verktøy", "bygg verktøy")), Label(None, Seq("bygg"))),
                     "nb")),
    1122,
    "content-import-client",
    updated,
    "verktoy"
  )

  val sampleCover2 = domain.Cover(
    Some(1),
    Some(1),
    Some(10001),
    "https://test.api.ndla.no/image-api/v1/raw/image.jpg",
    Seq(domain.Title("hammer2", "nb")),
    Seq(domain.Description("En hammer2 er et nyttig verktøy", "nb")),
    Seq(
      LanguageLabels(Seq(Label(Some("kategori"), Seq("jobbe verktøy", "mer label")), Label(None, Seq("byggherrer"))),
                     "nb"),
      LanguageLabels(Seq(Label(Some("kategori"), Seq("arbeids verktøy")), Label(None, Seq("byggkarer"))), "nn"),
      LanguageLabels(Seq(Label(Some("category"), Seq("work tools")), Label(None, Seq("workmen"))), "en")
    ),
    1122,
    "content-import-client",
    updated,
    "verktoy"
  )

  val sampleApiCover = api.Cover(
    1,
    1,
    "https://i.ndla.no/image-api/v1/raw/image.jpg",
    api.CoverTitle("hammer", "nb"),
    api.CoverDescription("En hammer er et nyttig verktøy", "nb"),
    1122,
    api.CoverLabels(Seq(api.Label(Some("kategori"), Seq("personlig verktøy")), api.Label(None, Seq("bygg"))), "nb"),
    Set("nb"),
    "content-import-client",
    updated,
    "verktoy",
    Some("https://ndla.no/node/10001")
  )

  val sampleApiNewCover = api.NewCover(
    language = "nb",
    "https://i.ndla.no/image-api/v1/raw/image.jpg",
    "hammer",
    "En hammer er et nyttig verktøy",
    1122,
    None,
    Seq(api.Label(Some("kategori"), Seq("personlig verktøy")), api.Label(None, Seq("bygg"))),
    "verktoy"
  )

  val sampleApiUpdateCover = api.UpdateCover(
    "nb",
    1,
    None,
    None,
    "hammer",
    "En hammer er et nyttig verktøy",
    Seq(api.Label(Some("kategori"), Seq("personlig verktøy")), api.Label(None, Seq("bygg"))),
    "verktoy"
  )

}
