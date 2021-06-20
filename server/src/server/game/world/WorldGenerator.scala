package server.game.world

import org.joml.Random

object WorldGenerator {
  private val random = new Random(123456l)
  def generate(): Unit = {
    println("Generating World")
    // for {
    //   x <- -45 until 45 by 2
    //   y <- -35 until 35 by 2
    // } yield {
    //   World.addFixture(FixtureSmallPatch(x, y, random.nextFloat() * Math.PI.toFloat * 2))
    // }
    for {
      x <- -45 until 45 by 1
      y <- -35 until 35 by 1
    } yield {
      World.addFixture(FixtureSmallGrass(x, y, 0))
    }
  }
}
