package loci
package communicator
package broadcastchannel

trait BroadcastChannel
    extends Protocol
    with SetupInfo
    with SecurityInfo
    with SymmetryInfo with Bidirectional {

  val name: String

  val authenticated: Boolean = true
  val encrypted: Boolean = true
  val integrityProtected: Boolean = true

  override def toString = s"BroadcastChannel($name)"
}

object BroadcastChannel extends BroadcastChannelSetupFactory {
  def unapply(broadcastChannel: BroadcastChannel) = Some((broadcastChannel.name))

  case class Properties()

  val schemes: Seq[String] = Seq()

  def apply(name: String): Connector[BroadcastChannel] =
    new BroadcastChannelConnector[BroadcastChannel](name, Properties())

  def apply(name: String, properties: Properties): Connector[BroadcastChannel] =
    new BroadcastChannelConnector[BroadcastChannel](name, properties)
}
