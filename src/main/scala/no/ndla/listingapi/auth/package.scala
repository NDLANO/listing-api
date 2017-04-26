/*
 * Part of NDLA listing_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.listingapi

import no.ndla.listingapi.model.api.AccessDeniedException
import no.ndla.network.AuthUser


package object auth {
  def assertHasRole(role: String): Unit = {
    if (!AuthUser.hasRole(role))
      throw new AccessDeniedException("User is missing required role to perform this operation")
  }

  def getUserId(): String = {
    println(s"##### AuthUser.get ${AuthUser.get}")
    println(s"##### AuthUser.get ${AuthUser.get.isEmpty}")
    println(s"##### AuthUser.get.get ${AuthUser.get.get.isEmpty}")
    if (AuthUser.get.isEmpty || AuthUser.get.get.isEmpty) {
      throw new AccessDeniedException(("User id required to perform this operation"))
    } else {
      return AuthUser.get.get
    }
  }
}
