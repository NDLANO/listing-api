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

  test("Language with empty string should convert language to unknown") {
    val lv = LanguageValue("", "This is ikke bare una Sprache")
    lv.lang should equal("unknown")
    lv.value should equal("This is ikke bare una Sprache")
    lv should equal(LanguageValue("unknown", "This is ikke bare una Sprache"))
  }

}
