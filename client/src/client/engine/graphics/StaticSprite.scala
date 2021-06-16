package client.engine.graphics

import scala.collection.immutable.HashMap
import org.joml.Math

class StaticSprite(
    val id: Int,
    val x: Float,
    val y: Float,
    val r: Float,
    val spriteSheet: SpriteSheet,
    val spriteName: String
) {
  // Build info from spritesheet
  val rect = spriteSheet.meta.get(spriteName).get
  val width = rect.width
  val height = rect.height

  // Create local copy of positions scaled to the local size of the sprite
  private val positions = Sprite.positions.clone()

  // Scale Positions
  for (i <- 0 until 8) {
    if (i % 2 == 0) {
      positions(i) *= width
    } else {
      positions(i) *= height
    }
  }

  // Rotate Positions
  private val s = Math.sin(-r)
  private val c = Math.cos(-r)
  for (i <- 0 until 4) {
    val ix = i * 2
    val iy = ix + 1
    val x = positions(ix)
    val y = positions(iy)
    positions(ix) = x * c - y * s
    positions(iy) = x * s + y * c
  }

  def getPositions(): Array[Float] = this.positions
  def getColors(): Array[Float] = Sprite.colors
  def getUvs(): Array[Float] = {
    val uvs = spriteSheet.getUVFromRect(rect)
    return Array[Float](
      uvs.x0,
      uvs.y0,
      uvs.x1,
      uvs.y0,
      uvs.x0,
      uvs.y1,
      uvs.x1,
      uvs.y1
    )
  }
  def getIndexes(): Array[Int] = Sprite.indexes
}

object Sprite {
  val positions = Array(
    -0.5f, -0.5f, 0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f
  )
  val colors = Array(
    0.5f, 0.5f, 0.0f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.8f, 0.5f, 0.0f, 0.5f
  )
  val uvs = Array(
    0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f
  )
  val indexes = Array[Int](
    0, 1, 2, 2, 1, 3
  )
}