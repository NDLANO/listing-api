package no.ndla.listingapi.model.domain

import no.ndla.listingapi.ListingApiProperties
import org.json4s.FieldSerializer
import org.json4s.FieldSerializer._
import org.json4s.native.Serialization._
import scalikejdbc._

case class ImageCoordinate(col: Long, row: Long)
case class CoverPhoto(imageApiId: Long, cropStart: ImageCoordinate, cropEnd: ImageCoordinate)
case class Label(`type`: Option[String], labels: Seq[String])
case class Card(id: Option[Long],
                coverPhotoUrl: String,
                title: String,
                description: String,
                labels: Seq[Label],
                articleId: Long
               )

object Card extends SQLSyntaxSupport[Card] {
  implicit val formats = org.json4s.DefaultFormats
  override val tableName = "cards"
  override val schemaName = Some(ListingApiProperties.MetaSchema)

  def apply(s: SyntaxProvider[Card])(rs:WrappedResultSet): Card = apply(s.resultName)(rs)
  def apply(s: ResultName[Card])(rs: WrappedResultSet): Card = {
    val meta = read[Card](rs.string(s.c("document")))
    Card(Some(rs.long(s.c("id"))), meta.coverPhotoUrl, meta.title, meta.description, meta.labels, meta.articleId)
  }

  val JSonSerializer = FieldSerializer[Card](
    ignore("id")
  )
}
