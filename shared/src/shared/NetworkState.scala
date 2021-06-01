package shared

import scala.collection.mutable.{Queue, HashMap}
import scala.collection.mutable.ArrayBuffer
import scala.reflect.{ClassTag, classTag}

/** Due to how Scala boxes types, recorded methods should always use Integer instead of Int in parameters
  */
abstract class NetworkStateBase extends Recordable {
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

  def setComponent(
      entityId: Integer,
      componentTag: String,
      component: HashMap[String, Any]
  ): Unit = {
    record("setComponent", entityId, componentTag, component)
    getEntityById(entityId).map(_.setComponent(componentTag, component))
  }

  /** Builds a list of patches to rebuild current state
    */
  def getFullStatePatches(): Array[String] = {
    val patches = new ArrayBuffer[String]()
    patches.addOne(PatchBuilder.build("setWorldName", worldName))
    entities.foreachEntry((id, networkEntity) => {
      patches.addOne(PatchBuilder.build("createEntity", id))
      networkEntity
        .getComponents()
        .foreachEntry((componentTag, component) => {
          patches.addOne(
            PatchBuilder.build("setComponent", id, componentTag, component)
          )
        })
    })
    return patches.toArray
  }
}

class NetworkEntity(val id: Int) {
  private val components = new HashMap[String, HashMap[String, Any]]()
  def getComponent(componentTag: String): Option[HashMap[String, Any]] =
    components.get(componentTag)
  def getComponents(): HashMap[String, HashMap[String, Any]] = components
  def setComponent(componentTag: String, map: HashMap[String, Any]): Unit = {
    components.put(componentTag, map)
  }
  def setComponentValue(componentTag: String, key: String, value: Any): Unit =
    components.get(componentTag).map(_.put(key, value))
  override def toString(): String = components.keys.mkString("\n\t")
}
