package server.game.world

import scala.collection.mutable.HashMap

class Grid[T] {
  private val cells = new HashMap[Int, HashMap[Int, T]]()
  def getCell(x: Int, y: Int): Option[T] = cells.get(x).flatMap(_.get(y))
  def getCellElseUpdate(x: Int, y: Int, default: => T): T = {
    val cell = getCell(x, y)
    if (cell.isEmpty) setCell(x, y, default)
    return getCell(x, y).get
  }
  def setCell(x: Int, y: Int, cell: T) =
    cells.getOrElseUpdate(x, new HashMap()).put(y, cell)
}
