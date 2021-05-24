package client.graphics

class StaticSprite(
    val id: Int,
    val x: Float,
    val y: Float,
    val width: Float = 32,
    val height: Float = 32
) {
  // Create local copy of positions scaled to the local size of the sprite
  private val positions = Sprite.positions.clone()
  for (i <- 0 until 8) {
    if (i % 2 == 0) {
      positions(i) *= width
    } else {
      positions(i) *= height
    }
  }
  def getPositions(): Array[Float] = this.positions
  def getColors(): Array[Float] = Sprite.colors
  def getUvs(): Array[Float] = Sprite.uvs
  def getIndexes(): Array[Int] = Sprite.indexes
}

object Sprite {
  val positions = Array(
    -0.5f, 0.5f, -0.5f, -0.5f, 0.5f, -0.5f, 0.5f, 0.5f
  )
  val colors = Array(
    0.5f, 0.5f, 0.0f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.8f, 0.5f, 0.0f, 0.5f
  )
  val uvs = Array(
    0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f
  )
  val indexes = Array[Int](
    0, 1, 2, 2, 3, 0
  )
}
