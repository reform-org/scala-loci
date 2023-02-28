package loci
package communicator
package ws

package object broadcastchannel {
  private[broadcastchannel] def unavailable = sys.error("BroadcastChannel only available in JavaScript")
}
