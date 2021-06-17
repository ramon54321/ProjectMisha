package client.engine.graphics

import org.lwjgl.system.MemoryStack
import org.lwjgl.stb.STBImage
import org.lwjgl.BufferUtils
import java.nio.ByteBuffer
import java.util.HashMap
import scala.util.Using
import scala.language.implicitConversions

import client.engine.Resources
import client.engine.ResourceError

case class Texture(
    val name: String,
    val width: Int,
    val height: Int,
    val channels: Int,
    val buffer: ByteBuffer
) {
  def blit(offsetX: Int, offsetY: Int, sourceTexture: Texture): Unit = {
    given byteFromInt: Conversion[Int, Byte] = _.asInstanceOf[Byte]
    val sourceWidth = sourceTexture.width
    val sourceHeight = sourceTexture.height
    val sourceBuffer = sourceTexture.buffer
    for (y <- 0 until sourceHeight) {
      for (x <- 0 until sourceWidth) {
        val targetY = y + offsetY
        val targetX = x + offsetX

        buffer.put(targetY * width * 4 + targetX * 4 + 0, 0x00)
        buffer.put(targetY * width * 4 + targetX * 4 + 1, 0x00)
        buffer.put(targetY * width * 4 + targetX * 4 + 2, 0x00)
        buffer.put(targetY * width * 4 + targetX * 4 + 3, 0xFF)

        buffer.put(targetY * width * 4 + targetX * 4 + 0, sourceBuffer.get(y * sourceWidth * 4 + x * 4 + 0))
        buffer.put(targetY * width * 4 + targetX * 4 + 1, sourceBuffer.get(y * sourceWidth * 4 + x * 4 + 1))
        buffer.put(targetY * width * 4 + targetX * 4 + 2, sourceBuffer.get(y * sourceWidth * 4 + x * 4 + 2))
        buffer.put(targetY * width * 4 + targetX * 4 + 3, sourceBuffer.get(y * sourceWidth * 4 + x * 4 + 3))
      }
    }
  }
}

object Textures {
  private val textures = new HashMap[String, Texture]()
  def get(name: String): Either[ResourceError, Texture] = {
    val path = f"/sprites/${name}"    
    Resources.load(path).map(resourceBuffer => {
      val texture = Using(MemoryStack.stackPush()) { stack =>
        val pWidth = stack.mallocInt(1)
        val pHeight = stack.mallocInt(1)
        val pChannels = stack.mallocInt(1)
        val buffer = STBImage.stbi_load_from_memory(
          resourceBuffer,
          pWidth,
          pHeight,
          pChannels,
          4
        )
        Texture(name, pWidth.get(0), pHeight.get(0), pChannels.get(0), buffer)
      }.get
      textures.put(name, texture)
      texture
    })      
  }

  def fromBytes(name: String, width: Int, height: Int, channels: Int, bytes: Array[Byte]): Texture = {
    val byteBuffer = BufferUtils
      .createByteBuffer(bytes.length)
      .put(bytes)
      .flip()
    val texture = Texture(name, width, height, channels, byteBuffer)
    textures.put(name, texture)
    return texture
  }
}
