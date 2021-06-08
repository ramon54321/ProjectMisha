package client.engine.graphics

import org.lwjgl.system.MemoryStack
import org.lwjgl.stb.STBImage
import org.lwjgl.BufferUtils
import java.nio.ByteBuffer
import java.util.HashMap
import scala.util.Using

case class Texture(
    val name: String,
    val width: Int,
    val height: Int,
    val channels: Int,
    val buffer: ByteBuffer
)

object Textures {
  private val textures = new HashMap[String, Texture]()
  def get(name: String): Texture = {
    Using(MemoryStack.stackPush()) { stack =>
      val resourceStreamBytes =
        getClass().getResourceAsStream(f"/${name}").readAllBytes()
      val resourceByteBuffer = BufferUtils
        .createByteBuffer(resourceStreamBytes.length)
        .put(resourceStreamBytes)
        .flip()
      val pWidth = stack.mallocInt(1)
      val pHeight = stack.mallocInt(1)
      val pChannels = stack.mallocInt(1)
      val buffer = STBImage.stbi_load_from_memory(
        resourceByteBuffer,
        pWidth,
        pHeight,
        pChannels,
        4
      )
      textures.put(
        name,
        Texture(name, pWidth.get(0), pHeight.get(0), pChannels.get(0), buffer)
      )
    }
    return textures.get(name)
  }
}
