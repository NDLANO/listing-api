/*
 * Part of NDLA listing_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.listingapi.auth

import no.ndla.listingapi.model.api.AccessDeniedException
import no.ndla.network.AuthUser

trait Client {

  val authClient: AuthClient

  class AuthClient {

    def client_id(): String = {
      assertHasClientId()
      return AuthUser.getClientId.get
    }

    def assertHasClientId(): Unit = {
      if (AuthUser.getClientId.isEmpty || AuthUser.getClientId.get.isEmpty) {
        throw new AccessDeniedException("Client id required to perform this operation")
      }
    }

  }

}
