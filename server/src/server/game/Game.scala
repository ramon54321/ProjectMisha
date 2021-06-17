package server.game

import server.engine.network.Network
import server.engine.NetworkState
import server.engine.ECS
import server.engine.Events
import server.engine.EVENT_START
import server.engine.EVENT_TICK
import server.game.world.WorldGenerator

object Game {
  Events.on[EVENT_START](_ => start())
  Events.on[EVENT_TICK](_ => tick())

  private def start() = {
    WorldGenerator.generate()
    NetworkState.setWorldName("Anderson")
    createEntity(0f, 0f)
    createEntity(10f, 20f)
  }

  private def tick() = {
    println("Tick")

    // Game Logic
    ECS.tick()

    // Send Network Updates
    val patches = NetworkState.dequeuePatches()
    patches.foreach(Network.broadcast)
  }

  private def createEntity(x: Float, y: Float) = {
    val entity = ECS.createEntity()
      .addComponent(new TransformComponent(x, y))
      .addComponent(new HealthComponent())
    NetworkState.createEntity(entity.id)
    entity.getComponents().foreach(_.updateNetworkState())
  }
}
