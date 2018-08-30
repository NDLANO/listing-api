/*
 * Part of NDLA listing_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package db.migration

import java.util.Date

import no.ndla.listingapi.{TestEnvironment, UnitSuite}

class V2__AddLanguageToAllTest extends UnitSuite with TestEnvironment {

  val migration = new V2__AddLanguageToAll

  test("add language to stuff with missing no language") {
    val before = V2_Cover(
      Some(1),
      Some(2),
      Some(5),
      "",
      Seq(V2_Title("Tittel", None)),
      Seq(V2_Description("Description", None)),
      Seq(
        V2_LanguageLabels(Seq(V2_Label(Some("cateogry"), Seq("betongfaget"))),
                          None)),
      1,
      "",
      new Date(),
      ""
    )

    val after = migration.updateCoverLanguage(before)

    after.title.forall(_.language.contains("unknown")) should be(true)
    after.labels.forall(_.language.contains("unknown")) should be(true)
    after.description.forall(_.language.contains("unknown")) should be(true)
  }

  test("add language to stuff with missing empty string as language") {
    val before = V2_Cover(
      Some(1),
      Some(2),
      Some(5),
      "",
      Seq(V2_Title("Tittel", Some(""))),
      Seq(V2_Description("Description", Some(""))),
      Seq(
        V2_LanguageLabels(Seq(V2_Label(Some("cateogry"), Seq("betongfaget"))),
                          Some(""))),
      1,
      "",
      new Date(),
      ""
    )

    val after = migration.updateCoverLanguage(before)
    after.title.forall(_.language.contains("unknown")) should be(true)
    after.labels.forall(_.language.contains("unknown")) should be(true)
    after.description.forall(_.language.contains("unknown")) should be(true)
  }

  test("existing languages should not be modified") {
    val before = V2_Cover(
      Some(1),
      Some(2),
      Some(5),
      "",
      Seq(V2_Title("Tittel", Some("nb"))),
      Seq(V2_Description("Description", Some("de"))),
      Seq(
        V2_LanguageLabels(Seq(V2_Label(Some("cateogry"), Seq("betongfaget"))),
                          Some("fr"))),
      1,
      "",
      new Date(),
      ""
    )

    val after = migration.updateCoverLanguage(before)
    after.title.forall(_.language.contains("nb")) should be(true)
    after.labels.forall(_.language.contains("fr")) should be(true)
    after.description.forall(_.language.contains("de")) should be(true)
  }

}
