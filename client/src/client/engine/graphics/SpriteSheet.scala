package client.engine.graphics

import scala.collection.immutable.HashMap

case class UV(val x0: Float, val y0: Float, val x1: Float, val y1: Float)

class SpriteSheet(
    val texture: Texture,
    val meta: HashMap[String, Rect]
) {
  def getUVFromRect(rect: Rect): UV = {
    val x0 = rect.x.toFloat
    val x1 = (rect.x + rect.width).toFloat / texture.width.toFloat
    val y0 = 1 - (rect.y.toFloat / texture.height.toFloat)
    val y1 = 1 - ((rect.y + rect.height).toFloat / texture.height.toFloat)
    return UV(x0, y0, x1, y1)
  }
}
