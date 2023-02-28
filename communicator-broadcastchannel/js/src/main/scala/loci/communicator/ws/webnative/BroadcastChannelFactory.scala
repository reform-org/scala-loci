package loci
package communicator
package broadcastchannel

import scala.concurrent.duration.FiniteDuration

private object BroadcastChannelSetupParser
    extends ConnectionSetupParser
    with SimpleConnectionSetupProperties {
  val self: BroadcastChannel.type = BroadcastChannel

  def properties(implicit props: ConnectionSetupFactory.Properties) =
    BroadcastChannel.Properties()
      .set[FiniteDuration]("heartbeat-delay") { v => _.copy(heartbeatDelay = v) }
      .set[FiniteDuration]("heartbeat-timeout") { v => _.copy(heartbeatTimeout = v) }
}

trait BroadcastChannelSetupFactory extends ConnectionSetupFactory.Implementation[BroadcastChannel] {
  val self: BroadcastChannel.type = BroadcastChannel

  val schemes = Seq("ws", "wss")

  protected def properties(
      implicit props: ConnectionSetupFactory.Properties): BroadcastChannel.Properties =
    BroadcastChannelSetupParser.properties

  protected def listener(
      url: String, scheme: String, location: String, properties: BroadcastChannel.Properties) =
    None

  protected def connector(
      url: String, scheme: String, location: String, properties: BroadcastChannel.Properties) =
    Some(BroadcastChannel(url, properties))
}
