package shared.engine

import scala.collection.mutable.Queue
import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag

object QueueUtils {
  def dequeueToArray[T:ClassTag](queue: Queue[T]): Array[T] = {
    val queueLength = queue.length
    val elements = new Array[T](queueLength)
    var i = 0
    while(i < queueLength) {
      elements(i) = queue.dequeue()
      i += 1
    }
    return elements
  }
}
