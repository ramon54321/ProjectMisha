package client.engine.graphics

import scala.collection.immutable.HashMap
import java.nio.ByteBuffer
import scala.collection.mutable

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
    val atlasTexture = Textures.fromBytes(
      name,
      4096,
      4096,
      4,
      Array.fill[Byte](4096 * 4096 * 4)(0x00.asInstanceOf[Byte])
    )

    var x512 = 0
    var x256 = 512
    var x128 = 1024
    var x64 = 2048
    var x32 = 3072

    var y512 = 0
    var y256 = 0
    var y128 = 0
    var y64 = 0
    var y32 = 0

    val meta = mutable.HashMap[String, Rect]()

    textures.foreach(texture => {
      texture.width match {
        case 512 => {
          if (y512 + texture.height > atlasTexture.height) throw new Exception(f"No atlas space to blit ${texture.height} texture")
          atlasTexture.blit(x512, y512, texture)
          meta.put(texture.name, Rect(x512, y512, texture.width, texture.height))
          y512 += texture.height
        }
        case 256 => {
          if (y256 + texture.height > atlasTexture.height)
            if x256 < x128 - 256
            then 
              x256 += 256
              y256 = 0
            else throw new Exception(f"No atlas space to blit ${texture.height} texture")
          atlasTexture.blit(x256, y256, texture)
          meta.put(texture.name, Rect(x256, y256, texture.width, texture.height))
          y256 += 256
        }
        case 128 => {
          if (y128 + texture.height > atlasTexture.height)
            if x128 < x64 - 128
            then 
              x128 += 128
              y128 = 0
            else throw new Exception(f"No atlas space to blit ${texture.height} texture")
          atlasTexture.blit(x128, y128, texture)
          meta.put(texture.name, Rect(x128, y128, texture.width, texture.height))
          y128 += 128
        }
        case 64 => {
          if (y64 + texture.height > atlasTexture.height)
            if x64 < x32 - 64
            then 
              x64 += 64
              y64 = 0
            else throw new Exception(f"No atlas space to blit ${texture.height} texture")
          atlasTexture.blit(x64, y64, texture)
          meta.put(texture.name, Rect(x64, y64, texture.width, texture.height))
          y64 += 64
        }
        case 32 => {
          if (y32 + texture.height > atlasTexture.height)
            if x32 < 4096 - 32
            then 
              x32 += 32
              y32 = 0
            else throw new Exception(f"No atlas space to blit ${texture.height} texture")
          atlasTexture.blit(x32, y32, texture)
          meta.put(texture.name, Rect(x32, y32, texture.width, texture.height))
          y32 += 32
        }
      }
    })
    return new SpriteSheet(
      atlasTexture,
      HashMap() ++ meta
    )
  }
}
