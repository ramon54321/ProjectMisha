package server.game.world

object WorldGenerator {
  def generate(): Unit = {
    println("Generating World")
    for {
      x <- -45 until 45
      y <- -35 until 35
    } yield {
      World.addFixture(FixtureSmallPatch(x, y))
    }
  }
}
