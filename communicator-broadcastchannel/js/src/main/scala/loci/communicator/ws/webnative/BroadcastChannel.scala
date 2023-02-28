package loci
package communicator
package broadcastchannel

import scala.concurrent.duration._

trait BroadcastChannel
    extends Protocol
    with SetupInfo
    with SecurityInfo
    with SymmetryInfo with Bidirectional {

  val name: String

  override def toString = s"BroadcastChannel($name)"
}

object BroadcastChannel extends BroadcastChannelSetupFactory {
  def unapply(broadcastChannel: BroadcastChannel) = Some((broadcastChannel.name))

  case class Properties()

  def apply(name: String): Connector[BroadcastChannel] =
    new BroadcastChannelConnector[BroadcastChannel](name, Properties())

  def apply(name: String, properties: Properties): Connector[BroadcastChannel] =
    new BroadcastChannelConnector[BroadcastChannel](name, properties)
}
