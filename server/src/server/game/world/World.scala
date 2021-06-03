package server.game.world

object World {
  val collisionGrid = new Grid[Boolean]()
  val fixtureGrid = new Grid[List[Int]]()
}
