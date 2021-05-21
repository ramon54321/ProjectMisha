package client

import shared.EventEmitter

object EventTag extends Enumeration {
  type EventTag = Value
  val Render, Ready = Value
}

object Events extends EventEmitter[EventTag.EventTag] {}
