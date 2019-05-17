/*
 * Copyright (C) 2016-2019 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.stream.alpakka.googlecloud.pubsub.impl

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.http.javadsl.ClientTransport
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.settings.{ClientConnectionSettings, ConnectionPoolSettings}

final object PubSubRequest {

  private val proxy: Option[ProxyObj] = {

    val proxyHost = sys.props.get("https.proxyHost");
    val proxyPort = sys.props.get("https.proxyPort");
    val nonProxyHosts = sys.props.get("http.nonProxyHosts");

    if (proxyHost.isDefined && proxyPort.isDefined) {
      nonProxyHosts.isDefined match {
        case true if (nonProxyHosts.get.indexOf("googleapis") != -1) => Option.empty
        case _ => Option.apply(ProxyObj(proxyHost.get, Integer.parseInt(proxyPort.get)))
      }
    } else {
      Option.empty
    }

  }

  private def poolSettings(implicit as: ActorSystem): ConnectionPoolSettings = {

    val clientTransport = proxy.isDefined match {
      case true =>
        ClientTransport.toScala(
          ClientTransport.httpsProxy(InetSocketAddress.createUnresolved(proxy.get.host, proxy.get.port))
        )
      case false => ClientConnectionSettings(as).transport
    }

    ConnectionPoolSettings(as)
      .withConnectionSettings(ClientConnectionSettings(as))
      .withTransport(clientTransport)
  }

  def singleRequest(req: HttpRequest)(implicit system: ActorSystem) =
    Http().singleRequest(req, settings = poolSettings)

  private case class ProxyObj(host: String, port: Int)

}
