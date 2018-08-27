package no.ndla.listingapi

package object service {

  def createOembedUrl(oldNodeId: Option[Long]): Option[String] = {
    oldNodeId match {
      case None     => createOembedUrl(null)
      case Some(id) => createOembedUrl(id)
    }
  }

  def createOembedUrl(oldNodeId: Long): Option[String] = {
    oldNodeId match {
      case id if id == null => None
      case _                => Option(s"https://ndla.no/node/$oldNodeId")
    }
  }
}
