package server.game.world

object WorldGenerator {
  def generate(): Unit = {
    println("Generating World")
    for {
      x <- -45 until 45
      y <- -35 until 35
    } yield {
      // TODO: Grid should fit smallest element... 32px?
      // TODO: Send fixture rotation over net
      World.addFixture(FixtureSmallPatch(x, y))
      World.addFixture(FixtureSmallGrass(x, y))
    }
  }
}
