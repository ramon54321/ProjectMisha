package server.game

import server.network.Network
import server.events.EventTag.EVENT_START
import server.events.EventTag.EVENT_TICK
import server.events.Events
import server.networkstate.NetworkState
import server.ecs.ECS
import server.ecs.HealthComponent

object Game {
  Events.on(EVENT_START, () => start())
  Events.on(EVENT_TICK, () => tick())

  private def start() = {
    NetworkState.setWorldName("Anderson")
    createEntity()
  }

  private def tick() = {
    println("Tick")

    // Game Logic
    ECS.tick()

    // Send Network Updates
    val patches = NetworkState.dequeuePatches()
    patches.foreach(Network.broadcast)
  }

  private def createEntity() = {
    val entity = ECS.createEntity().addComponent(new HealthComponent())
    NetworkState.createEntity(entity.id)
  }
}
