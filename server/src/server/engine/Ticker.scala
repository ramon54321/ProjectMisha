package server.engine

import server.engine.Events
import server.engine.EVENT_START
import server.engine.EVENT_TICK

object Ticker extends Thread {
  override def run(): Unit = {
    Events.emit(EVENT_START())
    while (true) {
      Thread.sleep(1000)
      Events.emit(EVENT_TICK())
    }
  }
}
