package loci
package communicator
package broadcastchannel

import scala.util.{Success, Try}

private sealed trait BroadcastChannelProtocolFactory[P <: BroadcastChannel] {
  def make(name: String): Try[P]
}

private object BroadcastChannelProtocolFactory {
  locally(BroadcastChannelProtocolFactory)

  implicit object broadcastchannel extends BroadcastChannelProtocolFactory[BroadcastChannel] {
    def make(name: String): Try[BroadcastChannel] =
      Success(construct(name))
  }

  private def construct(_name: String) =
    new BroadcastChannel {
      val name = _name;
    }
}
