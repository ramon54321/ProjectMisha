package shared

import java.util.HashMap
import scala.collection.mutable.ListBuffer

abstract class EventsBase[T] {
  private class Listener(val id: Int, val callback: (Seq[Any]) => Unit) {
    def apply(args: Seq[Any]) = {
      callback(args)
    }
  }

  private var id = 0
  private val events = new HashMap[T, ListBuffer[Listener]]()

  def on(eventTag: T, callback: () => Unit): Unit = {
    val _callback = (_: Seq[Any]) => callback()
    on(eventTag, _callback)
  }
  def on(eventTag: T, callback: (Seq[Any]) => Unit): Unit = {
    if (events.get(eventTag) == null) events.put(eventTag, new ListBuffer())
    events.get(eventTag).addOne(new Listener(id, callback))
    id += 1
  }

  def emit(eventTag: T): Unit = {
    emit(eventTag, Seq())
  }
  def emit(eventTag: T, args: Seq[Any]): Unit = {
    val listeners = events.get(eventTag)
    if (listeners == null) return
    listeners.map(listener => listener(args))
  }
}
