package server.ticker

import server.events.Events
import server.events.EventTag.EVENT_START
import server.events.EventTag.EVENT_TICK

object Ticker extends Thread {
  override def run(): Unit = {
    Events.emit(EVENT_START)
    while (true) {
      Thread.sleep(1000)
      Events.emit(EVENT_TICK)
    }
  }
}