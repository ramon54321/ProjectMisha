package server.game.world

import scala.collection.mutable.HashMap
import scala.collection.mutable.ArrayBuffer
import org.joml.Vector2ic

import shared.engine.IdUtils

object World {
  val collisionGrid = new Grid[Boolean]()
  val fixtureGrid = new Grid[ArrayBuffer[Fixture]]()

  val fixtures = new HashMap[Int, Fixture]()
  def addFixture(fixture: Fixture): Unit = {
    val x = fixture.position.x
    val y = fixture.position.y
    fixtures.put(fixture.id, fixture)
    fixtureGrid.getCellElseUpdate(x, y, new ArrayBuffer()).addOne(fixture)
    updateCollisionGrid(x, y)
  }

  private def updateCollisionGrid(x: Int, y: Int): Unit = {
    val fixtures = fixtureGrid.getCell(x, y)
    if (fixtures.isEmpty) {
      collisionGrid.setCell(x, y, false)
    } else {
      collisionGrid.setCell(x, y, !fixtures.get.forall(!_.isSolid))
    }
  }
}

abstract class Fixture(val id: Int, val position: Vector2ic, val isSolid: Boolean) {
  val tag: String = getClass().getCanonicalName()
}
case class FixtureSmallGrass(_id: Int, _position: Vector2ic) extends Fixture(_id, _position, isSolid = false)