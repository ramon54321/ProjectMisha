package shared

import scala.collection.mutable.Queue

object NetworkState extends Recordable {
  private var worldName: String = "Unknown"
  def getWorldName = worldName
  def setWorldName(value: String) = {
    record("setWorldName", value)
    worldName = value
  }
}
