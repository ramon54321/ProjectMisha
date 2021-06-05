package server.game

import scala.collection.mutable.HashMap

import server.engine.Component
import server.engine.NetworkState

class HealthComponent extends Component {
  def damage(amount: Int) = println("Damage")
  def updateNetworkState() = {
    println("Setting health component")
    NetworkState.setComponent(0, netTag, HashMap("Health" -> 75))
  }
}
