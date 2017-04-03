/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.listingapi.service

import com.netaporter.uri.Uri._
import no.ndla.listingapi.model.api.ValidationMessage
import no.ndla.listingapi.model.domain
import no.ndla.listingapi.model.domain.{Cover, ValidationException}
import no.ndla.mapping.ISO639.{get6391CodeFor6392Code, get6391CodeFor6392CodeMappings}
import org.jsoup.Jsoup
import org.jsoup.safety.Whitelist

import scala.util.{Failure, Success, Try}

trait CoverValidator {
  val coverValidator : CoverValidator

  class CoverValidator(titleRequired: Boolean = true, descriptionRequired: Boolean = true) {

    val MISSING_DESCRIPTION = "At least one description is required."
    val INVALID_COVER_PHOTO = "The url to the coverPhoto must point to an image in NDLA Image API."

    def validate(cover: Cover): Try[Cover] = {
      validateCover(cover) match {
        case head :: tail => Failure(new ValidationException(errors = head :: tail))
        case _ => Success(cover)
      }
    }

    private def validateCover(cover: domain.Cover) : Seq[ValidationMessage] = {
      cover.title.flatMap(validateTitle) ++
        cover.description.flatMap(validateDescription) ++
        cover.labels.flatMap(validateLanguageLabels) ++
        validateCoverPhoto(cover.coverPhotoUrl) ++
        cover.id.flatMap(id => validateId("id", id)) ++
        validateId("articleApiId", cover.articleApiId)
    }

    private def validateDescription(description: domain.Description): Seq[ValidationMessage] = {
      validateNoHtmlTags("description.description", description.description).toSeq ++
        description.language.flatMap(lang => validateLanguage("description.language", lang))
    }

    private def validateTitle(title: domain.Title): Seq[ValidationMessage] = {
      validateNoHtmlTags("title.title", title.title).toSeq ++
        title.language.flatMap(lang => validateLanguage("title.language", lang))
    }

    private def validateCoverPhoto(coverPhotoMetaUrl: String): Option[ValidationMessage] = {
      val parsedUrl = parse(coverPhotoMetaUrl)
      val host = parsedUrl.host

      val hostCorrect = host.getOrElse("").endsWith("ndla.no")
      val pathCorrect = parsedUrl.path.startsWith("/image-api/v")

      hostCorrect && pathCorrect match {
        case true => None
        case false => Some(ValidationMessage("coverPhotoMetaUrl", INVALID_COVER_PHOTO))
      }
    }

    private def validateId(fieldPath: String, id: Long): Option[ValidationMessage] = {
      id < 0 match {
        case true => Some(ValidationMessage(fieldPath, "The Id can not be less than zero"))
        case false => None
      }
    }

    private def validateLanguageLabels(labels: domain.LanguageLabels): Seq[ValidationMessage] = {
      labels.labels.flatMap(validateLabel) ++
        labels.language.flatMap(lang => validateLanguage("labels.language", lang))
    }

    private def validateLabel(label: domain.Label): Seq[ValidationMessage] = {
      label.labels.flatMap(l => validateNoHtmlTags("label.label", l)) ++
        label.`type`.flatMap(t => validateNoHtmlTags("label.type", t))
    }

    private def validateNoHtmlTags(fieldPath: String, text: String): Option[ValidationMessage] = {
      Jsoup.isValid(text, Whitelist.none()) match {
        case true => None
        case false => Some(ValidationMessage(fieldPath, "The content contains illegal html-characters. No HTML is allowed."))
      }
    }

    private def languageCodeSupported6391(languageCode: String): Boolean =
      get6391CodeFor6392CodeMappings.exists(_._2 == languageCode)

    private def validateLanguage(fieldPath: String, languageCode: String): Option[ValidationMessage] = {
      languageCodeSupported6391(languageCode) match {
        case true => None
        case false => Some(ValidationMessage(fieldPath, s"Language '$languageCode' is not a supported value."))
      }
    }

  }

}
