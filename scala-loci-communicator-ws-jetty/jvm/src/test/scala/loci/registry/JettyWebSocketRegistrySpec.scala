package loci
package registry

import communicator.ws.jetty._

import org.eclipse.jetty.server.{Server, ServerConnector}
import org.eclipse.jetty.servlet.ServletContextHandler
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class JettyWebSocketRegistrySpec extends AnyFlatSpec with Matchers with NoLogging {
  behavior of "Jetty WebSocket Registry"

  def run(test: RegistryTests.Test) = {
    val port = 45849

    val server = new Server()

    val connector = new ServerConnector(server)
    connector.setPort(port)

    val context = new ServletContextHandler(ServletContextHandler.SESSIONS)
    server.setHandler(context)
    server.addConnector(connector)

    test(WS(context, "/registry/*"), WS(s"ws://localhost:$port/registry/"),
      server.start(),
      (),
      server.stop())
  }

  it should "handle binding and lookup correctly" in {
    run(RegistryTests.`handle binding and lookup correctly`)
  }

  it should "handle subjective binding and lookup correctly" in {
    run(RegistryTests.`handle subjective binding and lookup correctly`)
  }
}
