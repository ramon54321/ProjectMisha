package server

import server.EventTag._

object Ticker extends Thread {
  override def run(): Unit = {
    ServerEvents.emit(EVENT_START)
    while (true) {
      Thread.sleep(1000)
      ServerEvents.emit(EVENT_TICK)
    }
  }
}
