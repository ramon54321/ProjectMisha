package server.engine

import shared.engine.EventsBase

sealed trait ServerEvent
case class EVENT_START() extends ServerEvent
case class EVENT_TICK() extends ServerEvent

object Events extends EventsBase[ServerEvent] {}
