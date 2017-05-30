/*
 * Part of NDLA listing_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.listingapi

import com.typesafe.scalalogging.LazyLogging
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{DefaultServlet, ServletContextHandler}
import org.scalatra.servlet.ScalatraListener

import scala.io.Source


object JettyLauncher extends LazyLogging {
  def buildLabelsCache = {
    ComponentRegistry.readService.uniqeLabelsMap("nb")
    ComponentRegistry.readService.uniqeLabelsMap("nn")
    ComponentRegistry.readService.uniqeLabelsMap("en")
  }

  def main(args: Array[String]) {
    logger.info(Source.fromInputStream(getClass.getResourceAsStream("/log-license.txt")).mkString)

    val source = ComponentRegistry.dataSource
    logger.info(s"Using datasource config $source")
    DBMigrator.migrate(source)

    val startMillis = System.currentTimeMillis()

    buildLabelsCache
    logger.info(s"Built tags cache in ${System.currentTimeMillis() - startMillis} ms.")

    val context = new ServletContextHandler()
    context setContextPath "/"
    context.addEventListener(new ScalatraListener)
    context.addServlet(classOf[DefaultServlet], "/")
    context.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false")

    val server = new Server(ListingApiProperties.ApplicationPort)
    server.setHandler(context)
    server.start()

    val startTime = System.currentTimeMillis() - startMillis
    logger.info(s"Started at port ${ListingApiProperties.ApplicationPort} in $startTime ms.")

    server.join()
  }
}
