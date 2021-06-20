package client.engine.graphics

import org.joml.Matrix4f

case class BatchRenderCompleteInfo(val submissions: Int, val flushes: Int)

class BatchRenderManager(val projectionMatrix: Matrix4f) {
  var activeBatchRenderer: BatchRenderer = null
  private var cameraX = 0f
  private var cameraY = 0f

  def setCameraPosition(x: Float, y: Float): Unit =
    cameraX = x
    cameraY = y

  private var submissions = 0
  def submitSprite(sprite: BatchSprite): Unit =
    if activeBatchRenderer != sprite.batchRenderer
    then
      if activeBatchRenderer != null then flush()
      activeBatchRenderer = sprite.batchRenderer
    if activeBatchRenderer.isFull() then flush()
    activeBatchRenderer.submitSprite(sprite)
    submissions += 1
  
  private var flushes = 0
  private def flush(): Unit =
    if activeBatchRenderer != null
    then activeBatchRenderer.flush(projectionMatrix, cameraX, cameraY)
    flushes += 1

  def complete(): BatchRenderCompleteInfo =
    flush()
    val completionInfo = BatchRenderCompleteInfo(submissions, flushes)
    flushes = 0
    submissions = 0
    completionInfo
}
