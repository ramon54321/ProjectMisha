package server.ticker

import server.events.ServerEvents
import server.events.EventTag.EVENT_START
import server.events.EventTag.EVENT_TICK

object Ticker extends Thread {
  override def run(): Unit = {
    ServerEvents.emit(EVENT_START)
    while (true) {
      Thread.sleep(1000)
      ServerEvents.emit(EVENT_TICK)
    }
  }
}
