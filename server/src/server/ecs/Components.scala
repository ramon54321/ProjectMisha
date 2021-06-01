package server.ecs

import server.networkstate.NetworkState
import scala.collection.mutable.HashMap

class HealthComponent extends Component {
  def damage(amount: Int) = println("Damage")
  def updateNetworkState() = {
    println("Setting health component")
    NetworkState.setComponent(0, tag, HashMap("Health" -> 75))
  }
}
