package server.ecs

import scala.reflect.{ClassTag, classTag}
import scala.collection.mutable.{ArrayBuffer, HashMap, HashSet}

object ECS {
  def tick() = {
    components.foreachEntry((tag, set) => {
      set.foreach(_.tick())
    })
  }

  private var idCounter = 0
  def createEntity(): Entity = {
    val entity = new Entity(idCounter)
    idCounter += 1
    return entity
  }
  private val components = new HashMap[String, HashSet[Component]]()
  def _registerComponent(component: Component) = {
    if (!components.contains(component.tag))
      components.put(component.tag, new HashSet())
    components.get(component.tag).get.add(component)
  }
}

class Entity(val id: Int) {
  private val components = new HashMap[String, Component]()
  def addComponent(component: Component): Entity = {
    components.put(component.tag, component)
    return this
  }
  def getComponent[T <: Component: ClassTag]: Option[T] = {
    val tag = classTag[T].runtimeClass.getCanonicalName()
    return components.get(tag).asInstanceOf[Option[T]]
  }
  def getComponents(): Iterable[Component] = components.values
}

abstract class Component {
  ECS._registerComponent(this)
  val tag: String = {
    val baseName = classOf[Component].getCanonicalName()
    val parents =
      new ArrayBuffer[String]().addOne(getClass().getCanonicalName())
    var parent = getClass().getSuperclass()
    while (parent.getCanonicalName != baseName) {
      parents.addOne(parent.getCanonicalName())
      parent = parent.getSuperclass()
    }
    parents.last
  }
  def tick(): Unit = {}
  def updateNetworkState(): Unit
}
