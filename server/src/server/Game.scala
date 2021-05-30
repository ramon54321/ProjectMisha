package server

import server.EventTag._
import server.ecs._

object Game {
  ServerEvents.on(EVENT_START, () => start())
  ServerEvents.on(EVENT_TICK, () => tick())

  private def start() = {
    ServerNetworkState.setWorldName("Anderson")
    createEntity()
  }

  private def tick() = {
    println("Tick")

    // Game Logic
    ECS.tick()

    // Send Network Updates
    val patches = ServerNetworkState.dequeuePatches()
    patches.foreach(Network.broadcast)
  }

  private def createEntity() = {
    val entity = ECS.createEntity().addComponent(new HealthComponent())
    ServerNetworkState.createEntity(entity.id)
  }
}
