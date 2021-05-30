package server.events

import shared.EventsBase

object EventTag extends Enumeration {
  type EventTag = Value
  val EVENT_START, EVENT_TICK =
    Value
}

object Events extends EventsBase[EventTag.EventTag] {}
