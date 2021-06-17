package server.game.world

import scala.collection.mutable.HashMap
import scala.collection.mutable.ArrayBuffer

import server.engine.NetworkState
import shared.engine.Grid

object World {
  val collisionGrid = new Grid[Boolean]()
  val fixtureGrid = new Grid[ArrayBuffer[Fixture]]()

  val fixtures = new HashMap[Int, Fixture]()
  def addFixture(fixture: Fixture): Unit = {
    // Add to World
    fixtures.put(fixture.id, fixture)
    fixtureGrid
      .getCellElseUpdate(fixture.x, fixture.y, new ArrayBuffer())
      .addOne(fixture)
    updateCollisionGrid(fixture.x, fixture.y)

    // Add to Network
    NetworkState.createFixture(fixture.id, fixture.tag, fixture.x, fixture.y, fixture.r, fixture.spriteName)
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
