package no.ndla.listingapi.model.domain

import no.ndla.listingapi.ListingApiProperties
import no.ndla.listingapi.model.api.NotFoundException
import no.ndla.listingapi.model.domain
import org.json4s.FieldSerializer
import org.json4s.FieldSerializer._
import org.json4s.native.Serialization._
import scalikejdbc._

import scala.util.{Failure, Success, Try}

case class Label(`type`: Option[String], labels: Seq[String])
case class LanguageLabels(labels: Seq[Label], language: Option[String]) extends LanguageField[Seq[Label]] { def data = labels }
case class Title(title: String, language: Option[String]) extends LanguageField[String] { def data = title }
case class Description(description: String, language: Option[String]) extends LanguageField[String] { def data = description }

case class Cover(id: Option[Long],
                 coverPhotoUrl: String,
                 title: Seq[Title],
                 description: Seq[Description],
                 labels: Seq[LanguageLabels],
                 articleApiId: Long
               ) {
  def getSupportedLanguages: Try[Seq[String]] = {
    val titleLangs = title.flatMap(_.language)
    val descriptionLangs = description.flatMap(_.language)
    val labelLangs = labels.flatMap(_.language)

    titleLangs == descriptionLangs && descriptionLangs == labelLangs match {
      case true => Success(titleLangs)
      case false => Failure(new NotFoundException)
    }
  }
}

object Cover extends SQLSyntaxSupport[Cover] {
  implicit val formats = org.json4s.DefaultFormats
  override val tableName = "covers"
  override val schemaName = Some(ListingApiProperties.MetaSchema)

  def apply(s: SyntaxProvider[Cover])(rs:WrappedResultSet): Cover = apply(s.resultName)(rs)
  def apply(s: ResultName[Cover])(rs: WrappedResultSet): Cover = {
    val meta = read[Cover](rs.string(s.c("document")))
    Cover(Some(rs.long(s.c("id"))), meta.coverPhotoUrl, meta.title, meta.description, meta.labels, meta.articleApiId)
  }

  val JSonSerializer = FieldSerializer[Cover](
    ignore("id")
  )
}
