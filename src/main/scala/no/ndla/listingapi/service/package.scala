package no.ndla.listingapi

package object service {
  def createOembedUrl(oldNodeId: Long): String = s"https://ndla.no/node/$oldNodeId"
}
