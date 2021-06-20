package client.engine.graphics

import org.joml.Matrix4fc

abstract trait BatchRenderer(val spriteSheet: SpriteSheet) {
  def isFull(): Boolean
  def submitSprite(sprite: BatchSprite): Unit
  def flush(projectionMatrix: Matrix4fc, cameraX: Float, cameraY: Float): Unit
}