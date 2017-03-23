package no.ndla.listingapi.model.domain

import no.ndla.listingapi.ListingApiProperties
import org.json4s.FieldSerializer
import org.json4s.FieldSerializer._
import org.json4s.native.Serialization._
import scalikejdbc._

case class Label(`type`: Option[String], labels: Seq[String])
case class Cover(id: Option[Long],
                 coverPhotoUrl: String,
                 title: String,
                 description: String,
                 labels: Seq[Label],
                 articleId: Long
               )

object Cover extends SQLSyntaxSupport[Cover] {
  implicit val formats = org.json4s.DefaultFormats
  override val tableName = "covers"
  override val schemaName = Some(ListingApiProperties.MetaSchema)

  def apply(s: SyntaxProvider[Cover])(rs:WrappedResultSet): Cover = apply(s.resultName)(rs)
  def apply(s: ResultName[Cover])(rs: WrappedResultSet): Cover = {
    val meta = read[Cover](rs.string(s.c("document")))
    Cover(Some(rs.long(s.c("id"))), meta.coverPhotoUrl, meta.title, meta.description, meta.labels, meta.articleId)
  }

  val JSonSerializer = FieldSerializer[Cover](
    ignore("id")
  )
}
