package no.ndla.listingapi

package object service {
  def createOembedUrl(oldNodeId: Option[Long]): Option[String] = {
    oldNodeId match {
      case None => None
      case Some(id) => Option(s"https://ndla.no/node/$id/embed")
    }
  }
}
