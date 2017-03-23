package no.ndla.listingapi.repository

import no.ndla.listingapi.integration.DataSource
import no.ndla.listingapi.model.domain.{Cover, Label}
import org.json4s.native.Serialization.write
import org.postgresql.util.PGobject
import scalikejdbc._

trait ListingRepository {
  this: DataSource =>
  val listingRepository: ListingRepository

  class ListingRepository {
    implicit val formats = org.json4s.DefaultFormats

    def insertCover(cover: Cover)(implicit session: DBSession = AutoSession): Cover = {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(cover))

      val imageId = sql"insert into ${Cover.table} (document) values (${dataObject})".updateAndReturnGeneratedKey.apply
      cover.copy(id=Some(imageId))
    }

    def getCover(coverId: Long)(implicit session: DBSession = ReadOnlyAutoSession): Option[Cover] = {
      coverWhere(sqls"c.id = $coverId")
    }

    def deleteCover(coverId: Long)(implicit session: DBSession = AutoSession) = {
      sql"delete from ${Cover.table} where id = $coverId".update.apply
    }

    private def coverWhere(whereClause: SQLSyntax)(implicit session: DBSession = ReadOnlyAutoSession): Option[Cover] = {
      val c = Cover.syntax("c")
      sql"select ${c.result.*} from ${Cover.as(c)} where $whereClause".map(Cover(c)).single.apply
    }

  }
}
