package shared.engine

object IdUtils {
  private var _id = -1
  def generateId(): Int = {
    _id += 1
    _id
  }
}
