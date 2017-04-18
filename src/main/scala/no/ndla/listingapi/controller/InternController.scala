/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.listingapi.controller

import no.ndla.listingapi.model.api.{NewCover, UpdateCover}
import no.ndla.listingapi.service.WriteService
import no.ndla.listingapi.service.search.SearchIndexService
import org.scalatra.{InternalServerError, Ok}

import scala.util.{Failure, Success}

trait InternController {
  this:  SearchIndexService with WriteService =>
  val internController: InternController

  class InternController extends NdlaController {

    post("/index") {
      searchIndexService.indexDocuments match {
        case Success(reindexResult) => {
          val result = s"Completed indexing of ${reindexResult.totalIndexed} documents in ${reindexResult.millisUsed} ms."
          logger.info(result)
          Ok(result)
        }
        case Failure(f) => {
          logger.warn(f.getMessage, f)
          InternalServerError(f.getMessage)
        }
      }
    }

    post("/newcover") {
      writeService.newCover(extract[NewCover](request.body))
    }

    put("/updatecover/:coverid") {
      writeService.updateCover(long("coverid"), extract[UpdateCover](request.body))
    }

  }
}
