package shared.engine

import scala.collection.mutable.{HashMap, ArrayBuffer}
import scala.reflect.{ClassTag, classTag}

abstract class EventsBase[E] {
  type Action[T <: E] = (event: T) => Unit

  private val events = new HashMap[String, ArrayBuffer[E => Unit]]()

  def emit[T <: E](event: T): Unit = {
    events.get(event.getClass.getName).map(actions => actions.foreach(action => action(event)))
  }

  def on[T <: E : ClassTag](action: T => Unit): Unit = {
    val key = classTag[T].runtimeClass.getName
    events.getOrElseUpdate(key, new ArrayBuffer()).addOne(action.asInstanceOf[E => Unit])
  }
}
