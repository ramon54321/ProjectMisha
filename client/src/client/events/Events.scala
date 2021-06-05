package client.events

import shared.EventsBase

sealed trait ClientEvent
case class EVENT_GL_READY() extends ClientEvent
case class EVENT_GL_RENDER() extends ClientEvent
case class EVENT_GL_UPDATE() extends ClientEvent
case class EVENT_TICKER_SECOND() extends ClientEvent

object Events extends EventsBase[ClientEvent] {}
