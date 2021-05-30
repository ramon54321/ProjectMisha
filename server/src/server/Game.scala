package server

import server.EventTag._
import server.ecs.ECS

object Game {
  ServerEvents.on(EVENT_TICK, () => tick())

  private def tick() = {
    println("Tick")
    
    // Game Logic
    ECS.tick()
    ServerNetworkState.setWorldName("Anderson")

    // Send Network Updates
    val patches = ServerNetworkState.dequeuePatches()
    patches.foreach(Network.broadcast)
  }
}
