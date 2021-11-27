package loci
package transmitter

import _root_.rescala.interface.RescalaInterface

class RescalaTransmitter[I <: RescalaInterface](val interface: I) extends
  transmittable.SignalTransmittable with
  transmittable.EventTransmittable with
  SignalAccessor with
  EventAccessor with
  RescalaGateway

object rescala extends RescalaTransmitter[_root_.rescala.default.type](_root_.rescala.default)
