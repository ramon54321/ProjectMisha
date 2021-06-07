package shared.game

import scala.collection.mutable.{Queue, HashMap}
import scala.collection.mutable.ArrayBuffer

import shared.engine.Recordable
import shared.engine.RecordableMode
import shared.engine.PatchBuilder

/** Due to how Scala boxes types, recorded methods should always use Integer instead of Int in parameters
  */
abstract class NetworkStateBase(mode: RecordableMode) extends Recordable(mode) {
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
      netTag: String,
      component: HashMap[String, Any]
  ): Unit = {
    record("setComponent", entityId, netTag, component)
    getEntityById(entityId).map(_.setComponent(netTag, component))
  }

  private val fixtures = new HashMap[Int, NetworkFixture]
  def getFixtureById(id: Int): Option[NetworkFixture] = fixtures.get(id)
  def getFixtures(): Iterable[NetworkFixture] = fixtures.values
  def createFixture(id: Integer, netTag: String, x: Integer, y: Integer): Unit = {
    record("createFixture", id, netTag, x, y)
    fixtures.put(id, NetworkFixture(id, netTag, x, y))
  }

  /** Builds a list of patches to rebuild current state
    */
  override def getRebuildPatches(): Array[String] = {
    val patches = new ArrayBuffer[String]()
    patches.addOne(PatchBuilder.build("setWorldName", worldName))
    entities.foreachEntry((id, networkEntity) => {
      patches.addOne(PatchBuilder.build("createEntity", id))
      networkEntity
        .getComponents()
        .foreachEntry((netTag, component) => {
          patches.addOne(
            PatchBuilder.build("setComponent", id, netTag, component)
          )
        })
    })
    fixtures.foreachEntry((id, networkFixture) => {
      patches.addOne(
        PatchBuilder.build(
          "createFixture",
          id,
          networkFixture.netTag,
          networkFixture.x,
          networkFixture.y
        )
      )
    })
    return patches.toArray
  }
}

class NetworkEntity(val id: Int) {
  private val components = new HashMap[String, HashMap[String, Any]]()
  def getComponent(netTag: String): Option[HashMap[String, Any]] =
    components.get(netTag)
  def getComponents(): HashMap[String, HashMap[String, Any]] = components
  def setComponent(netTag: String, map: HashMap[String, Any]): Unit = {
    components.put(netTag, map)
  }
  def setComponentValue(netTag: String, key: String, value: Any): Unit =
    components.get(netTag).map(_.put(key, value))
  override def toString(): String = components.keys.mkString("\n\t")
}

case class NetworkFixture(
    val id: Int,
    val netTag: String,
    val x: Integer,
    val y: Integer
)
