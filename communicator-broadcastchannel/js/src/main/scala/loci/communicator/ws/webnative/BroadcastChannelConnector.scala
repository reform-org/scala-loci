package loci
package communicator
package broadcastchannel

import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.timers._
import scala.scalajs.js.typedarray.ArrayBuffer
import scala.util.{Failure, Success}

@js.native
@JSGlobal("BroadcastChannel")
class JSBroadcastChannel(name: String) extends js.Object {
  def postMessage(value: js.Any): Unit = js.native

  def addEventListener(`type`: String, callback: js.Function1[dom.MessageEvent, _]): Unit = js.native
}

private class BroadcastChannelConnector[P <: BroadcastChannel: BroadcastChannelProtocolFactory](
  name: String, properties: BroadcastChannel.Properties)
    extends Connector[P] {

  protected def connect(connectionEstablished: Connected[P]) = {
    val bc = new JSBroadcastChannel(name)
    
    implicitly[BroadcastChannelProtocolFactory[P]].make(name) match {
      case Failure(exception) =>
        connectionEstablished.set(Failure(exception))

      case Success(ws) =>

        val doClosed = Notice.Steady[Unit]
        val doReceive = Notice.Stream[MessageBuffer]

        val connection = new Connection[P] {
          val protocol = ws
          val closed = doClosed.notice
          val receive = doReceive.notice

          def open = bc.readyState == dom.WebSocket.OPEN
          def send(data: MessageBuffer) = {
            bc.send(data.backingArrayBuffer)
            resetInterval.fire()
          }
          def close() = bc.close()
        }

        connectionEstablished.set(Success(connection))

        bc.onmessage = { (event: dom.MessageEvent) =>
          event.data match {
            case data: ArrayBuffer =>
              doReceive.fire(MessageBuffer wrapArrayBuffer data)

            case data: dom.Blob =>
              val reader = new dom.FileReader
              reader.onload = { (event: dom.Event) =>
                doReceive.fire(MessageBuffer wrapArrayBuffer
                  event.target.asInstanceOf[js.Dynamic].result.asInstanceOf[ArrayBuffer])
              }
              reader.readAsArrayBuffer(data)

            case _ =>
          }
        }
    }
  }
}
