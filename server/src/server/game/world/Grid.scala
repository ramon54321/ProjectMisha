package server.game.world

import scala.collection.mutable.HashMap

class Grid[T] {
  private val cells = new HashMap[Int, HashMap[Int, T]]()
  def getCell(x: Int, y: Int): Option[T] = cells.get(x).flatMap(_.get(y))
  def setCell(x: Int, y: Int, cell: T) =
    cells.getOrElseUpdate(x, new HashMap()).put(y, cell)
}
