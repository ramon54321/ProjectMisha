package client

import shared.EventEmitter

object EventTag extends Enumeration {
  type EventTag = Value
  val EVENT_GL_READY, EVENT_GL_RENDER, EVENT_GL_UPDATE = Value
}

object Events extends EventEmitter[EventTag.EventTag] {}
