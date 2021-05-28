package shared

import scala.collection.mutable.Queue

abstract class NetworkState extends Recordable {
  private var worldName: String = "Unknown"
  def getWorldName = worldName
  def setWorldName(value: String) = {
    record("setWorldName", value)
    worldName = value
  }
}
