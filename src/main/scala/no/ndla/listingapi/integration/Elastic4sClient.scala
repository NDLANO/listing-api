/*
 * Part of NDLA listing_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.listingapi.integration

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.{Region, Regions}
import com.netaporter.uri.dsl._
import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.aws._
import com.sksamuel.elastic4s.http.{HttpClient, HttpExecutable, RequestSuccess}
import java.util.concurrent.Executors

import no.ndla.listingapi.ListingApiProperties.{RunWithSignedSearchRequests, SearchServer}
import no.ndla.listingapi.model.api.NdlaSearchException
import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.apache.http.protocol.HttpContext
import org.apache.http.{HttpRequest, HttpRequestInterceptor}
import org.elasticsearch.client.RestClientBuilder.{HttpClientConfigCallback, RequestConfigCallback}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor}
import scala.util.{Failure, Success, Try}

trait Elastic4sClient {
  val e4sClient: NdlaE4sClient
}

case class NdlaE4sClient(httpClient: HttpClient) {

  def execute[T, U](request: T)(implicit exec: HttpExecutable[T, U]): Try[RequestSuccess[U]] = {
    implicit val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor)
    val response = Await
      .ready(httpClient.execute {
        request
      }, Duration.Inf)
      .value
      .get

    response match {
      case Success(either) =>
        either match {
          case Right(result)        => Success(result)
          case Left(requestFailure) => Failure(NdlaSearchException(requestFailure))
        }
      case Failure(ex) => Failure(ex)
    }
  }
}

object Elastic4sClientFactory {

  def getClient(searchServer: String = SearchServer): NdlaE4sClient = {
    RunWithSignedSearchRequests match {
      case true  => NdlaE4sClient(getSigningClient(searchServer))
      case false => NdlaE4sClient(getNonSigningClient(searchServer))
    }
  }

  private object RequestConfigCallbackWithTimeout extends RequestConfigCallback {
    override def customizeRequestConfig(requestConfigBuilder: RequestConfig.Builder): RequestConfig.Builder = {
      val elasticSearchRequestTimeoutMs = 10000
      requestConfigBuilder.setConnectionRequestTimeout(elasticSearchRequestTimeoutMs)
    }
  }

  private object HttpClientCallbackWithAwsInterceptor extends HttpClientConfigCallback {
    override def customizeHttpClient(httpClientBuilder: HttpAsyncClientBuilder): HttpAsyncClientBuilder = {
      httpClientBuilder.addInterceptorLast(new AwsHttpInterceptor)
    }
  }

  /** This is the same code as [[Aws4ElasticClient]] uses,
    * however Aws4ElasticClient does not expose a way to configure [[RequestConfigCallback]] */
  private class AwsHttpInterceptor extends HttpRequestInterceptor {
    private val defaultChainProvider = new DefaultAWSCredentialsProviderChain
    private val region = sys.env("AWS_DEFAULT_REGION")
    private val signer = new Aws4RequestSigner(defaultChainProvider, region)

    override def process(request: HttpRequest, context: HttpContext): Unit = signer.withAws4Headers(request)
  }

  private def getNonSigningClient(searchServer: String): HttpClient = {
    val uri = ElasticsearchClientUri(searchServer.host.getOrElse("localhost"), searchServer.port.getOrElse(9200))
    HttpClient(uri, requestConfigCallback = RequestConfigCallbackWithTimeout)
  }

  private def getSigningClient(searchServer: String): HttpClient = {
    val elasticSearchUri =
      s"elasticsearch://${searchServer.host.getOrElse("localhost")}:${searchServer.port.getOrElse(80)}?ssl=false"

    val awsRegion = Option(Regions.getCurrentRegion).getOrElse(Region.getRegion(Regions.EU_CENTRAL_1)).toString
    setEnv("AWS_DEFAULT_REGION", awsRegion)

    HttpClient(
      ElasticsearchClientUri(elasticSearchUri),
      httpClientConfigCallback = HttpClientCallbackWithAwsInterceptor,
      requestConfigCallback = RequestConfigCallbackWithTimeout
    )
  }

  private def setEnv(key: String, value: String) = {
    val field = System.getenv().getClass.getDeclaredField("m")
    field.setAccessible(true)
    val map = field.get(System.getenv()).asInstanceOf[java.util.Map[java.lang.String, java.lang.String]]
    map.put(key, value)
  }
}
