/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.listingapi.caching

import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}

import com.typesafe.scalalogging.LazyLogging
import no.ndla.listingapi.ListingApiProperties.ApiClientsCacheAgeInMs

class Memoize[R](maxCacheAgeMs: Long, f: () => R, autoRefreshCache: Boolean) extends (() => R) with LazyLogging {

  private[this] var cache: Option[CacheValue] = None

  def apply(): R = {
    cache match {
      case Some(cachedValue) if autoRefreshCache => cachedValue.value
      case Some(cachedValue) if !cachedValue.isExpired => cachedValue.value
      case _ =>
        renewCache
        cache.get.value
    }
  }

  def renewCache :Unit =  {
    cache = Some(CacheValue(f(), System.currentTimeMillis()))
    logger.debug(s"cache renewd ${cache.get}")
  }

  if (autoRefreshCache) {
    val ex = new ScheduledThreadPoolExecutor(1)
    val task = new Runnable {
      def run() = renewCache
    }
    ex.scheduleAtFixedRate(task, 20, maxCacheAgeMs, TimeUnit.MILLISECONDS)
  }

  case class CacheValue(value: R, lastUpdated: Long) {
    def isExpired: Boolean = lastUpdated + maxCacheAgeMs <= System.currentTimeMillis()
  }

}

object Memoize {
  def apply[R](f: () => R) = new Memoize(ApiClientsCacheAgeInMs, f, autoRefreshCache = false)
}

object MemoizeAutoRenew {
  def apply[R](f: () => R) = new Memoize(ApiClientsCacheAgeInMs, f, autoRefreshCache = true)
}
