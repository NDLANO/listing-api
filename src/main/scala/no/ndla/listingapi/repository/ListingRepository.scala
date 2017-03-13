package no.ndla.listingapi.repository

import no.ndla.listingapi.integration.DataSource
import no.ndla.listingapi.model.domain.{Card, Label}
import org.json4s.native.Serialization.write
import org.postgresql.util.PGobject
import scalikejdbc._

trait ListingRepository {
  this: DataSource =>
  val listingRepository: ListingRepository

  class ListingRepository {
    implicit val formats = org.json4s.DefaultFormats

    def insertCard(card: Card)(implicit session: DBSession = AutoSession): Card = {
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(write(card))

      val imageId = sql"insert into ${Card.table} (document) values (${dataObject})".updateAndReturnGeneratedKey.apply
      card.copy(id=Some(imageId))
    }

    def getCard(cardId: Long)(implicit session: DBSession = ReadOnlyAutoSession): Option[Card] = {
      cardWhere(sqls"c.id = $cardId")
    }

    def deleteCard(cardId: Long)(implicit session: DBSession = AutoSession) = {
      sql"delete from ${Card.table} where id = $cardId".update.apply
    }

    private def cardWhere(whereClause: SQLSyntax)(implicit session: DBSession = ReadOnlyAutoSession): Option[Card] = {
      val c = Card.syntax("c")
      sql"select ${c.result.*} from ${Card.as(c)} where $whereClause".map(Card(c)).single.apply
    }

  }
}
