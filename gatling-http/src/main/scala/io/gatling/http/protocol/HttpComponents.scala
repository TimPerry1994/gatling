/**
 * Copyright 2011-2017 GatlingCorp (http://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.protocol

import io.gatling.core.protocol.ProtocolComponents
import io.gatling.core.session.Session
import io.gatling.http.ahc.{ AhcChannelPoolPartitionSelector, HttpEngine, ResponseProcessor }
import io.gatling.http.cache.HttpCaches

import org.asynchttpclient.DefaultAsyncHttpClient

case class HttpComponents(httpProtocol: HttpProtocol, httpEngine: HttpEngine, httpCaches: HttpCaches, responseProcessor: ResponseProcessor) extends ProtocolComponents {

  private val onExitF: Session => Unit = session => {
    val (_, ahc: DefaultAsyncHttpClient) = httpEngine.httpClient(session, httpProtocol)
    ahc.getChannelPool.flushPartitions(new AhcChannelPoolPartitionSelector(session.userId))
  }

  override def onStart: Option[Session => Session] =
    Some(httpCaches.setNameResolver(httpProtocol, httpEngine)
      andThen httpCaches.setLocalAddress(httpProtocol)
      andThen httpCaches.setBaseUrl(httpProtocol)
      andThen httpCaches.setWsBaseUrl(httpProtocol))

  override def onExit: Option[Session => Unit] = Some(onExitF)
}
