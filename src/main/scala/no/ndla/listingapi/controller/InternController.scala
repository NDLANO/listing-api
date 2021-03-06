/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.listingapi.controller

import no.ndla.listingapi.service.{ReadService, WriteService}
import no.ndla.listingapi.service.search.SearchIndexService
import org.scalatra.{InternalServerError, Ok}

import scala.util.{Failure, Success}

trait InternController {
  this: SearchIndexService with WriteService with ReadService =>
  val internController: InternController

  class InternController extends NdlaController {

    post("/index") {
      searchIndexService.indexDocuments match {
        case Success(reindexResult) => {
          val result =
            s"Completed indexing of ${reindexResult.totalIndexed} documents in ${reindexResult.millisUsed} ms."
          logger.info(result)
          Ok(result)
        }
        case Failure(f) => {
          logger.warn(f.getMessage, f)
          InternalServerError(f.getMessage)
        }
      }
    }

    get("/dump/cover") {
      readService.getCoverDump()
    }

  }
}
