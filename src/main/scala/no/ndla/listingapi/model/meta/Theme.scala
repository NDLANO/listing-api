package no.ndla.listingapi.model.meta

//The meta theme of a group av covers. Correspond to the wanted separation of listings which is not
// to be used in the filtering functionality. Filtering of covers happens within a theme in the client.

object Theme {
  val allowedThemes :List [String] = List("naturbruk", "verktoy")
}
