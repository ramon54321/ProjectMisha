package client.engine.graphics

import org.joml.Matrix4fc

abstract trait BatchRenderer {
  def flush(projectionMatrix: Matrix4fc, cameraX: Float, cameraY: Float): Unit 
}