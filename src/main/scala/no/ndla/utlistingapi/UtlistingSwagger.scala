/*
 * Part of NDLA utlisting_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.utlistingapi

import org.scalatra.ScalatraServlet
import org.scalatra.swagger._

class ResourcesApp(implicit val swagger: Swagger) extends ScalatraServlet with NativeSwaggerBase {
  get("/") {
    renderSwagger2(swagger.docs.toList)
  }
}

object UtlistingApiInfo {
  val apiInfo = ApiInfo(
  "Utlisting Api",
  "Documentation for the Utlisting API of NDLA.no",
  "https://ndla.no",
  UtlistingApiProperties.ContactEmail,
  "GPL v3.0",
  "http://www.gnu.org/licenses/gpl-3.0.en.html")
}

class UtlistingSwagger extends Swagger("2.0", "0.8", UtlistingApiInfo.apiInfo) {
  addAuthorization(OAuth(List("utlisting:all"), List(ApplicationGrant(TokenEndpoint("/auth/tokens", "access_token")))))
}
