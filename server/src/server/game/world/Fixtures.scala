package server.game.world

import shared.engine.IdUtils

case class FixtureSmallGrass(override val x: Int, override val y: Int)
    extends Fixture(x, y, isSolid = false)
case class FixtureSmallBush(override val x: Int, override val y: Int)
    extends Fixture(x, y, isSolid = true)

abstract sealed class Fixture(val x: Int, val y: Int, val isSolid: Boolean) {
  val id: Int = IdUtils.generateId()
  val tag: String = getClass().getCanonicalName().split('.').last
}
