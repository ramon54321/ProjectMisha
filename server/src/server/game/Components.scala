package server.game

import scala.collection.mutable.HashMap

import server.engine.Component
import server.engine.NetworkState

class TransformComponent(private var x: Float, private var y: Float) extends Component {
  private var dir = 1
  override def tick(): Unit = {
    if dir > 0 && x > 20 then dir = -1
    if dir < 0 && x < -20 then dir = 1
    x += dir * 2
    updateNetworkState()
  }
  override def updateNetworkState(): Unit = {
    println("Setting transform component")
    NetworkState.setComponent(entity.id, netTag, HashMap("x" -> x, "y" -> y))
  }
}

class HealthComponent extends Component {
  def damage(amount: Int): Unit = println("Damage")
  override def updateNetworkState(): Unit = {
    println("Setting health component")
    NetworkState.setComponent(entity.id, netTag, HashMap("Health" -> 75))
  }
}
