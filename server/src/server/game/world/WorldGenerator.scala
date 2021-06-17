package server.game.world

import org.joml.Random

object WorldGenerator {
  private val random = new Random(123456l)
  def generate(): Unit = {
    println("Generating World")
    for {
      x <- -45 until 45
      y <- -35 until 35
    } yield {
      // TODO: Grid should fit smallest element... 32px?
      World.addFixture(FixtureSmallPatch(x, y, random.nextFloat() * Math.PI.toFloat * 2))
      World.addFixture(FixtureSmallGrass(x, y, 0))
    }
  }
}
