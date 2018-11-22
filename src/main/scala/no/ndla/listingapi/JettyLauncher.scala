/*
 * Part of NDLA listing_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.listingapi

import java.util

import com.typesafe.scalalogging.LazyLogging
import javax.servlet.DispatcherType
import net.bull.javamelody.{MonitoringFilter, Parameter, ReportServlet, SessionListener}
import no.ndla.listingapi.model.domain.{Lang, UniqeLabels}
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{DefaultServlet, FilterHolder, ServletContextHandler}
import org.scalatra.servlet.ScalatraListener

import scala.io.Source

object JettyLauncher extends LazyLogging {

  def buildLabelsCache: Map[Lang, UniqeLabels] = {
    ComponentRegistry.readService.allLabelsMap()
  }

  def main(args: Array[String]) {
    logger.info(
      Source
        .fromInputStream(getClass.getResourceAsStream("/log-license.txt"))
        .mkString)

    val source = ComponentRegistry.dataSource
    DBMigrator.migrate(source)

    val startMillis = System.currentTimeMillis()

    buildLabelsCache
    logger.info(s"Built tags cache in ${System.currentTimeMillis() - startMillis} ms.")

    val context = new ServletContextHandler()
    context setContextPath "/"
    context.addEventListener(new ScalatraListener)
    context.addServlet(classOf[DefaultServlet], "/")
    context.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false")

    context.addServlet(classOf[ReportServlet], "/monitoring")
    context.addEventListener(new SessionListener)
    val monitoringFilter = new FilterHolder(new MonitoringFilter())
    monitoringFilter.setInitParameter(Parameter.APPLICATION_NAME.getCode, ListingApiProperties.ApplicationName)
    ListingApiProperties.Environment match {
      case "local" => None
      case _ =>
        monitoringFilter.setInitParameter(Parameter.CLOUDWATCH_NAMESPACE.getCode,
                                          "NDLA/APP".replace("APP", ListingApiProperties.ApplicationName))
    }
    context.addFilter(monitoringFilter, "/*", util.EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC))

    val server = new Server(ListingApiProperties.ApplicationPort)
    server.setHandler(context)
    server.start()

    val startTime = System.currentTimeMillis() - startMillis
    logger.info(s"Started at port ${ListingApiProperties.ApplicationPort} in $startTime ms.")

    server.join()
  }
}
