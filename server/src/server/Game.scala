package server

import shared.NetworkState

object Game extends Thread {
  override def run(): Unit = {
    println("Game Running...")
    while (true) {
      Thread.sleep(1000)
      println("Tick")

      // Game Logic
      NetworkState.setWorldName("Anderson")

      // Send Network Updates
      val patches = NetworkState.dequeuePatches()
      patches.foreach(Network.broadcast)
    }
  }
}