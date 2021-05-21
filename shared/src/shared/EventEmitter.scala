package shared

import java.util.HashMap
import scala.collection.mutable.ListBuffer

trait EventEmitter[T] {
  private class Listener(val id: Int, val callback: () => Unit) {
    def apply() = {
      callback()
    }
  }

  private var id = 0
  private val events = new HashMap[T, ListBuffer[Listener]]()

  def on(eventTag: T, callback: () => Unit) = {
    if (events.get(eventTag) == null) events.put(eventTag, new ListBuffer())
    events.get(eventTag).addOne(new Listener(id, callback))
    id += 1
  }

  def emit(eventTag: T): Unit = {
    val listeners = events.get(eventTag)
    if (listeners == null) return
    listeners.map(listener => listener())
  }
}
