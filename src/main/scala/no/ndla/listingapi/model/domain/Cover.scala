/*
 * Part of NDLA listing_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.listingapi.model.domain

import java.util.Date

import no.ndla.listingapi.ListingApiProperties
import no.ndla.listingapi.model.api.NotFoundException
import org.json4s.FieldSerializer
import org.json4s.FieldSerializer.ignore
import org.json4s.native.Serialization._
import scalikejdbc._

import scala.util.{Failure, Success, Try}

case class Cover(id: Option[Long],
  revision: Option[Int],
  oldNodeId: Option[Long],
  coverPhotoUrl: String,
  title: Seq[Title],
  description: Seq[Description],
  labels: Seq[LanguageLabels],
  articleApiId: Long,
  updatedBy: String,
  updated: Date,
  theme: ThemeName
) {
  lazy val supportedLanguages: Set[String] = {
    val titleLangs = title.map(_.language)
    val descriptionLangs = description.map(_.language)
    val labelLangs = labels.map(_.language)

    titleLangs.union(descriptionLangs).union(labelLangs).toSet
  }
}

object Cover extends SQLSyntaxSupport[Cover] {
  implicit val formats = org.json4s.DefaultFormats
  override val tableName = "covers"
  override val schemaName = Some(ListingApiProperties.MetaSchema)
  val JSonSerializer = FieldSerializer[Cover](
    ignore("id") orElse
      ignore("revision")
  )

  def apply(s: SyntaxProvider[Cover])(rs: WrappedResultSet): Cover = apply(s.resultName)(rs)

  def apply(s: ResultName[Cover])(rs: WrappedResultSet): Cover = {
    val meta = read[Cover](rs.string(s.c("document")))
    Cover(Some(rs.long(s.c("id"))),
      Some(rs.int(s.c("revision"))),
      meta.oldNodeId,
      meta.coverPhotoUrl,
      meta.title,
      meta.description,
      meta.labels,
      meta.articleApiId,
      meta.updatedBy,
      meta.updated,
      meta.theme
    )
  }
}
