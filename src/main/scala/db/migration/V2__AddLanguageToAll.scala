/*
 * Part of NDLA listing_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package db.migration

import java.sql.Connection
import java.util.Date

import no.ndla.listingapi.model.domain.search.Language
import org.flywaydb.core.api.migration.jdbc.JdbcMigration
import org.json4s.FieldSerializer
import org.json4s.FieldSerializer.ignore
import org.json4s.native.Serialization.{read, write}
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

class V2__AddLanguageToAll extends JdbcMigration {
  implicit val formats = org.json4s.DefaultFormats + FieldSerializer[V2_Cover](ignore("id"))

  override def migrate(connection: Connection): Unit = {
    val db = DB(connection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      allImages.map(updateCoverLanguage).foreach(update)
    }
  }

  def updateCoverLanguage(cover: V2_Cover): V2_Cover = {
    cover.copy(
      title = cover.title.map(t => V2_Title(t.title, Some(Language.languageOrUnknown(t.language)))),
      description = cover.description.map(t => V2_Description(t.description, Some(Language.languageOrUnknown(t.language)))),
      labels = cover.labels.map(t => V2_LanguageLabels(t.labels, Some(Language.languageOrUnknown(t.language))))
    )
  }

  def allImages(implicit session: DBSession): List[V2_Cover] = {
    sql"select id, document, revision from covers".map(rs => {
      val meta = read[V2_Cover](rs.string("document"))
      V2_Cover(
        Some(rs.long("id")),
        Some(rs.int("revision")),
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
    ).list().apply()
  }

  def update(cover: V2_Cover)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(write(cover))

    sql"update covers set document = $dataObject where id = ${cover.id}".update().apply
  }

}

case class V2_Cover(id: Option[Long],
                 revision: Option[Int],
                 oldNodeId: Option[Long],
                 coverPhotoUrl: String,
                 title: Seq[V2_Title],
                 description: Seq[V2_Description],
                 labels: Seq[V2_LanguageLabels],
                 articleApiId: Long,
                 updatedBy: String,
                 updated: Date,
                 theme: String)

case class V2_Title(title: String, language: Option[String])
case class V2_Description(description: String, language: Option[String])
case class V2_LanguageLabels(labels: Seq[V2_Label], language: Option[String])
case class V2_Label(`type`: Option[String], labels: Seq[String])
