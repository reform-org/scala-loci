package loci
package communicator
package tcp

import scala.concurrent.duration._

trait TCP extends
    Protocol with
    SetupInfo with
    SecurityInfo with
    SymmetryInfo with Bidirectional {
  val host: String
  val port: Int

  override def toString = s"TCP($host, $port)"
}

object TCP extends TCPSetupFactory {
  def unapply(tcp: TCP) = Some((tcp.host, tcp.port))

  case class Properties(
    heartbeatDelay: FiniteDuration = 3.seconds,
    heartbeatTimeout: FiniteDuration = 10.seconds)

  private def ??? = sys error "TCP communicator only available on the JVM"

  def apply(port: Int): Listener[TCP] = ???
  def apply(port: Int, interface: String): Listener[TCP] = ???
  def apply(port: Int, properties: Properties): Listener[TCP] = ???
  def apply(port: Int, interface: String, properties: Properties): Listener[TCP] = ???

  def apply(host: String, port: Int): Connector[TCP] = ???
  def apply(host: String, port: Int, properties: Properties): Connector[TCP] = ???
}

trait TCPSetupFactory extends
    ConnectionSetupFactory[TCP] { this: TCP.type =>
  val schemes = Seq("tcp")

  protected def properties(implicit props: ConnectionSetupFactory.Properties) =
    Properties()
  protected def listener(url: String, scheme: String, location: String, properties: Properties) =
    None
  protected def connector(url: String, scheme: String, location: String, properties: Properties) =
    None
}
