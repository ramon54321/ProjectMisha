package server.game.world

object WorldGenerator {
  def generate(): Unit = {
    println("Generating World")

    World.addFixture(FixtureSmallGrass(0, 0))
  }
}
