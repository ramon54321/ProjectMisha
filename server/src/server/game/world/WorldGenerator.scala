package server.game.world

object WorldGenerator {
  def generate(): Unit = {
    println("Generating World")

    for {
      x <- -25 until 25
      y <- -15 until 15
    } yield {
      World.addFixture(FixtureSmallGrass(x, y))
    }
  }
}
