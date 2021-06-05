package shared.engine

import scala.collection.mutable.Queue

enum RecordableMode:
  case Writer, Reader

trait Recordable(val mode: RecordableMode) {

  // object Events extends EventsBase[String] {}

  def getRebuildPatches(): Array[String]

  def applyPatch(patch: String): Boolean = {
    if (mode == RecordableMode.Writer) return false
    val segments = patch.split('|')
    val args = segments.tail.map(Marshal.parse)
    val argTypes = args.map(_.getClass())
    val method = getClass().getMethod(segments(0), argTypes: _*)
    method.invoke(this, args: _*)
    // TODO: Replace with new events system
    // Events.emit(segments(0), args)
    return true
  }

  private val patchQueue = new Queue[String]()
  protected def record(methodName: String, args: Any*): Boolean = {
    if (mode == RecordableMode.Reader) return false
    val patch = PatchBuilder.build(methodName, args: _*)
    patchQueue.enqueue(patch)
    return true
  }
  def dequeuePatches(): Array[String] = QueueUtils.dequeueToArray(patchQueue)
}

object PatchBuilder {
  def build(methodName: String, args: Any*): String = {
    val stringifiedArgs = args.map(Marshal.serialize)
    return Seq(methodName).concat(stringifiedArgs).mkString("|")
  }
}
