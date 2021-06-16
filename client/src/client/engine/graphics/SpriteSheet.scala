package client.engine.graphics

import scala.collection.immutable.HashMap
import java.nio.ByteBuffer

case class UV(val x0: Float, val y0: Float, val x1: Float, val y1: Float)

class SpriteSheet(
    val texture: Texture,
    val meta: HashMap[String, Rect]
) {
  def getUVFromRect(rect: Rect): UV = {
    val x0 = rect.x.toFloat / texture.width.toFloat
    val x1 = (rect.x + rect.width).toFloat / texture.width.toFloat
    val y0 = (rect.y + rect.height).toFloat / texture.height.toFloat
    val y1 = rect.y.toFloat / texture.height.toFloat
    return UV(x0, y0, x1, y1)
  }
}

object SpriteSheet {
  def fromTextures(name: String, textures: Array[Texture]): SpriteSheet = {
    val texture = Textures.fromBytes(
      name,
      512,
      512,
      4,
      Array.fill[Byte](512 * 512 * 4)(0x00.asInstanceOf[Byte])
    )
    texture.blit(0, 0, textures(0))
    texture.blit(128, 0, textures(1))
    return new SpriteSheet(
      texture,
      HashMap(
        textures(0).name -> Rect(0, 0, textures(0).width, textures(0).height),
        textures(1).name -> Rect(128, 0, textures(1).width, textures(1).height)
      )
    )
  }
}
