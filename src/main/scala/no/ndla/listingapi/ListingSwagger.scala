/*
 * Part of NDLA listing_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.listingapi

import org.scalatra.ScalatraServlet
import org.scalatra.swagger._

class ResourcesApp(implicit val swagger: Swagger) extends ScalatraServlet with NativeSwaggerBase {
  get("/") {
    renderSwagger2(swagger.docs.toList)
  }
}

object ListingApiInfo {
  val apiInfo = ApiInfo(
  "Listing Api",
  "Documentation for the Listing API of NDLA.no",
  "https://ndla.no",
  ListingApiProperties.ContactEmail,
  "GPL v3.0",
  "http://www.gnu.org/licenses/gpl-3.0.en.html")
}

class ListingSwagger extends Swagger("2.0", "0.8", ListingApiInfo.apiInfo) {
  val roleWithWriteAccessInTest = ListingApiProperties.RoleWithWriteAccess.replace(":", "-test:")
  addAuthorization(OAuth(List(roleWithWriteAccessInTest), List(ImplicitGrant(LoginEndpoint(ListingApiProperties.Auth0LoginEndpoint),"access_token"))))
}
