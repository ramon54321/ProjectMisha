package server.events

import shared.EventsBase

sealed trait ServerEvent
case class EVENT_START() extends ServerEvent
case class EVENT_TICK() extends ServerEvent

object Events extends EventsBase[ServerEvent] {}
