/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */
package no.ndla.listingapi.service.search

import no.ndla.listingapi.model.domain.search.LanguageValue
import no.ndla.listingapi.{TestEnvironment, UnitSuite}

class SearchableCoverTest extends UnitSuite with TestEnvironment {

  test("Language with empty Some should convert language to None") {
    val lv = LanguageValue(Some(""), "This is ikke bare una Sprache")
    lv.lang should equal(None)
    lv.value should equal("This is ikke bare una Sprache")
    lv should equal(LanguageValue(None, "This is ikke bare una Sprache"))
  }

}
