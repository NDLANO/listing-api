package no.ndla.listingapi.repository

import com.typesafe.scalalogging.LazyLogging
import no.ndla.listingapi.integration.DataSource
import no.ndla.listingapi.model.api.OptimisticLockException
import no.ndla.listingapi.model.domain.Cover
import org.json4s.native.Serialization.write
import org.postgresql.util.PGobject
import scalikejdbc._

import scala.util.{Failure, Success, Try}

trait ListingRepository {
  this: DataSource =>
  val listingRepository: ListingRepository

  class ListingRepository extends LazyLogging {
    implicit val formats = org.json4s.DefaultFormats + Cover.JSonSerializer

    def insertCover(cover: Cover)(implicit session: DBSession = AutoSession): Cover = {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(cover))

      val startRevision = 1
      val coverId = sql"insert into ${Cover.table} (document, revision) values (${dataObject}, $startRevision)".updateAndReturnGeneratedKey.apply
      cover.copy(id=Some(coverId), revision=Some(startRevision))
    }

    def updateCover(cover: Cover)(implicit session: DBSession = AutoSession): Try[Cover] = {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(cover))

      val newRevision = cover.revision.getOrElse(0) + 1
      val count = sql"update ${Cover.table} set document=${dataObject}, revision=$newRevision where id=${cover.id} and revision=${cover.revision}".update.apply

      if (count != 1) {
        val message = s"Found revision mismatch when attempting to update article ${cover.id}"
        logger.info(message)
        Failure(new OptimisticLockException)
      } else {
        logger.info(s"Updated cover ${cover.id}")
        Success(cover.copy(revision=Some(newRevision)))
      }
    }

    def getCover(coverId: Long)(implicit session: DBSession = ReadOnlyAutoSession): Option[Cover] = {
      coverWhere(sqls"c.id = $coverId")
    }

    def getCoverWithOldNodeId(oldNodeId: Long)(implicit session: DBSession = ReadOnlyAutoSession): Option[Cover] = {
      coverWhere(sqls"c.document->>'oldNodeId' = ${oldNodeId.toString}")
    }

    def cardsWithIdBetween(min: Long, max: Long): List[Cover] = coversWhere(sqls"c.id between $min and $max").toList

    def minMaxId(implicit session: DBSession = AutoSession): (Long, Long) = {
      sql"select coalesce(MIN(id),0) as mi, coalesce(MAX(id),0) as ma from ${Cover.table}".map(rs => {
        (rs.long("mi"), rs.long("ma"))
      }).single().apply() match {
        case Some(minmax) => minmax
        case None => (0L, 0L)
      }
    }

    def deleteCover(coverId: Long)(implicit session: DBSession = AutoSession) = {
      sql"delete from ${Cover.table} where id = $coverId".update.apply
    }

    private def coverWhere(whereClause: SQLSyntax)(implicit session: DBSession = ReadOnlyAutoSession): Option[Cover] = {
      val c = Cover.syntax("c")
      sql"select ${c.result.*} from ${Cover.as(c)} where $whereClause".map(Cover(c)).single.apply
    }

    private def coversWhere(whereClause: SQLSyntax)(implicit session: DBSession = ReadOnlyAutoSession): Seq[Cover] = {
      val c = Cover.syntax("c")
      sql"select ${c.result.*} from ${Cover.as(c)} where $whereClause".map(Cover(c)).list.apply
    }

  }
}
