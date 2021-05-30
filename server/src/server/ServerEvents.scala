package server

import shared.Events

object EventTag extends Enumeration {
  type EventTag = Value
  val EVENT_TICK =
    Value
}

object ServerEvents extends Events[EventTag.EventTag] {}