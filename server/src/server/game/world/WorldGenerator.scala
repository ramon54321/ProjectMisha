package server.game.world

import server.engine.NetworkState
import shared.engine.IdUtils

object WorldGenerator {
  def generate(): Unit = {
    println("Generating World")
    
    NetworkState.createFixture(IdUtils.generateId())
    NetworkState.createFixture(IdUtils.generateId())
    NetworkState.createFixture(IdUtils.generateId())
    NetworkState.createFixture(IdUtils.generateId())
  }
}
