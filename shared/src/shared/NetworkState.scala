package shared

import scala.collection.mutable.{Queue, HashMap}
import scala.collection.mutable.ArrayBuffer

/** Due to how Scala boxes types, recorded methods should always use Integer instead of Int in parameters
  */
abstract class NetworkState extends Recordable {
  private var worldName: String = "Unknown"
  def getWorldName = worldName
  def setWorldName(value: String): Unit = {
    record("setWorldName", value)
    worldName = value
  }

  private val entities = new HashMap[Int, NetworkEntity]
  def getEntityById(id: Int): Option[NetworkEntity] = entities.get(id)
  def getEntities(): Iterable[NetworkEntity] = entities.values
  def createEntity(id: Integer): Unit = {
    record("createEntity", id)
    entities.put(id, new NetworkEntity(id))
  }

  /** Builds a list of patches to rebuild current state
    */
  def getFullStatePatches(): Array[String] = {
    val patches = new ArrayBuffer[String]()
    patches.addOne(PatchBuilder.build("setWorldName", worldName))
    entities.foreachEntry((id, networkEntity) =>
      patches.addOne(PatchBuilder.build("createEntity", id))
    )
    return patches.toArray
  }
}

case class NetworkEntity(val id: Int) {}
