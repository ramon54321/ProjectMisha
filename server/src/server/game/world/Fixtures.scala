package server.game.world

import shared.engine.IdUtils

case class FixtureSmallGrass(override val x: Int, override val y: Int, override val r: Float)
    extends Fixture(x, y, r, spriteName = "grass1.png", isSolid = false)
case class FixtureSmallPatch(override val x: Int, override val y: Int, override val r: Float)
    extends Fixture(x, y, r, spriteName = "patch1.png", isSolid = true)

abstract sealed class Fixture(val x: Int, val y: Int, val r: Float, val spriteName: String, val isSolid: Boolean) {
  val id: Int = IdUtils.generateId()
  val tag: String = getClass().getCanonicalName().split('.').last
}
