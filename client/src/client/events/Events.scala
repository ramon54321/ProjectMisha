package client.events

import shared.EventsBase

object EventTag extends Enumeration {
  type EventTag = Value
  val EVENT_GL_READY, EVENT_GL_RENDER, EVENT_GL_UPDATE, EVENT_TICKER_SECOND =
    Value
}

object Events extends EventsBase[EventTag.EventTag] {}
