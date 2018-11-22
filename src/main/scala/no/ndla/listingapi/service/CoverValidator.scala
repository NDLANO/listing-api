/*
 * Part of NDLA learningpath_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.listingapi.service

import io.lemonlabs.uri.Url
import io.lemonlabs.uri.dsl._
import no.ndla.listingapi.model.api.{ValidationException, ValidationMessage}
import no.ndla.listingapi.model.domain
import no.ndla.listingapi.model.domain.Cover
import no.ndla.listingapi.model.meta.Theme
import no.ndla.mapping.ISO639.get6391CodeFor6392CodeMappings
import org.jsoup.Jsoup
import org.jsoup.safety.Whitelist

import scala.util.{Failure, Success, Try}

trait CoverValidator {
  val coverValidator: CoverValidator

  class CoverValidator(titleRequired: Boolean = true, descriptionRequired: Boolean = true) {

    val MISSING_DESCRIPTION = "At least one description is required."

    val INVALID_COVER_PHOTO =
      "The url to the coverPhoto must point to an image in NDLA Image API."

    def validate(cover: Cover): Try[Cover] = {
      validateCover(cover) match {
        case head :: tail =>
          Failure(new ValidationException(errors = head :: tail))
        case _ => Success(cover)
      }
    }

    private def validateCover(cover: domain.Cover): Seq[ValidationMessage] = {
      cover.title.flatMap(validateTitle) ++
        cover.description.flatMap(validateDescription) ++
        cover.labels.flatMap(validateLanguageLabels) ++
        validateCoverPhoto(cover.coverPhotoUrl) ++
        cover.id.flatMap(id => validateId("id", id)) ++
        validateId("articleApiId", cover.articleApiId) ++
        validateTheme(cover.theme)
    }

    private def validateDescription(description: domain.Description): Seq[ValidationMessage] = {
      validateNoHtmlTags("description.description", description.description).toSeq ++
        validateLanguage("description.language", description.language)
    }

    private def validateTitle(title: domain.Title): Seq[ValidationMessage] = {
      validateNoHtmlTags("title.title", title.title).toSeq ++
        validateLanguage("title.language", title.language)
    }

    private def validateCoverPhoto(coverPhotoMetaUrl: String): Option[ValidationMessage] = {
      val parsedUrl = Url.parse(coverPhotoMetaUrl)
      val host = parsedUrl.hostOption.map(_.toString).getOrElse("")

      val hostCorrect = host.endsWith("ndla.no") || host.endsWith("ndla-local")
      val pathCorrect = parsedUrl.path.toString.startsWith("/image-api/")

      hostCorrect && pathCorrect match {
        case true => None
        case false =>
          Some(ValidationMessage("coverPhotoMetaUrl", INVALID_COVER_PHOTO + " " + host))
      }
    }

    private def validateId(fieldPath: String, id: Long): Option[ValidationMessage] = {
      id < 0 match {
        case true =>
          Some(ValidationMessage(fieldPath, "The Id can not be less than zero"))
        case false => None
      }
    }

    private def validateLanguageLabels(labels: domain.LanguageLabels): Seq[ValidationMessage] = {
      labels.labels.flatMap(validateLabel) ++
        validateLanguage("labels.language", labels.language)
    }

    private def validateLabel(label: domain.Label): Seq[ValidationMessage] = {
      label.labels.flatMap(l => validateNoHtmlTags("label.label", l)) ++
        label.`type`.flatMap(t => validateNoHtmlTags("label.type", t))
    }

    private def validateNoHtmlTags(fieldPath: String, text: String): Option[ValidationMessage] = {
      Jsoup.isValid(text, Whitelist.none()) match {
        case true => None
        case false =>
          Some(ValidationMessage(fieldPath, "The content contains illegal html-characters. No HTML is allowed."))
      }
    }

    private def languageCodeSupported6391(languageCode: String): Boolean =
      get6391CodeFor6392CodeMappings.exists(_._2 == languageCode)

    private def validateLanguage(fieldPath: String, languageCode: String): Option[ValidationMessage] = {
      languageCodeSupported6391(languageCode) match {
        case true => None
        case false =>
          Some(ValidationMessage(fieldPath, s"Language '$languageCode' is not a supported value."))
      }
    }

    private def validateTheme(name: domain.ThemeName): Option[ValidationMessage] = {
      Theme.allowedThemes.contains(name.toLowerCase) match {
        case true => None
        case false =>
          Some(ValidationMessage(name, s"Theme name '$name' is not a supportet theme."))
      }
    }

  }

}
