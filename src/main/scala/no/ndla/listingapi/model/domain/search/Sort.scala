/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.listingapi.model.domain.search

object Sort  extends Enumeration {
  val ByIdDesc = Value("-id")
  val ByIdAsc = Value("id")

  def valueOf(s:String): Option[Sort.Value] = {
    Sort.values.find(_.toString == s)
  }

  def valueOf(s:Option[String]): Option[Sort.Value] = {
    s match {
      case None => None
      case Some(s) => valueOf(s)
    }
  }
}

