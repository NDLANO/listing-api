/*
 * Part of NDLA listing_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.listingapi

import com.typesafe.scalalogging.LazyLogging
import no.ndla.network.{AuthUser, Domains}
import no.ndla.network.secrets.PropertyKeys
import no.ndla.network.secrets.Secrets.readSecrets

import scala.util.Properties._
import scala.util.{Failure, Success}

object ListingApiProperties extends LazyLogging {
  val Environment = propOrElse("NDLA_ENVIRONMENT", "local")
  val ApplicationName = "listing-api"

  val Auth0LoginEndpoint =
    s"https://${AuthUser.getAuth0HostForEnv(Environment)}/authorize"

  val RoleWithWriteAccess = "listing:write"
  val SecretsFile = "listing-api.secrets"

  val ApplicationPort = propOrElse("APPLICATION_PORT", "80").toInt
  val ContactEmail = "christergundersen@ndla.no"
  lazy val Domain: String = Domains.get(Environment)

  val SearchServer =
    propOrElse("SEARCH_SERVER", "http://search-listing-api.ndla-local")
  val SearchRegion = propOrElse("SEARCH_REGION", "eu-central-1")

  val RunWithSignedSearchRequests =
    propOrElse("RUN_WITH_SIGNED_SEARCH_REQUESTS", "true").toBoolean
  val SearchIndex = propOrElse("SEARCH_INDEX_NAME", "listings")
  val SearchDocument = "listing"
  val DefaultPageSize = 100
  val MaxPageSize = 100
  val IndexBulkSize = 200

  val DefaultLanguage = "nb"

  lazy val MetaUserName = prop(PropertyKeys.MetaUserNameKey)
  lazy val MetaPassword = prop(PropertyKeys.MetaPasswordKey)
  lazy val MetaResource = prop(PropertyKeys.MetaResourceKey)
  lazy val MetaServer = prop(PropertyKeys.MetaServerKey)
  lazy val MetaPort = prop(PropertyKeys.MetaPortKey).toInt
  lazy val MetaSchema = prop(PropertyKeys.MetaSchemaKey)
  val MetaInitialConnections = 3
  val MetaMaxConnections = 20

  val CorrelationIdKey = "correlationID"
  val CorrelationIdHeader = "X-Correlation-ID"

  val ApiClientsCacheAgeInMs: Long = 1000 * 60 * 60 // 1 hour caching
  val ElasticSearchIndexMaxResultWindow = 10000

  lazy val secrets = readSecrets(SecretsFile) match {
    case Success(values) => values
    case Failure(exception) =>
      throw new RuntimeException(s"Unable to load remote secrets from $SecretsFile", exception)
  }

  def booleanProp(key: String) = prop(key).toBoolean

  def prop(key: String): String = {
    propOrElse(key, throw new RuntimeException(s"Unable to load property $key"))
  }

  def propOrElse(key: String, default: => String): String = {
    secrets.get(key).flatten match {
      case Some(secret) => secret
      case None =>
        envOrNone(key) match {
          case Some(env) => env
          case None      => default
        }
    }
  }
}
