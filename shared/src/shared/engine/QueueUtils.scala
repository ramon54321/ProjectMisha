package shared.engine

import scala.collection.mutable.Queue
import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag

object QueueUtils {
  def dequeueToArray[T:ClassTag](queue: Queue[T]): Array[T] = {
    val elements = new Array[T](queue.length)
    var i = 0
    while(queue.length > 0) {
      elements(i) = queue.dequeue()
      i += 1
    }
    return elements
  }
}
