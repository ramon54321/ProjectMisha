package client.engine.graphics

import org.lwjgl.system.MemoryStack
import org.lwjgl.stb.STBImage
import java.nio.ByteBuffer
import java.util.HashMap
import scala.util.Using
import java.io.File

object Textures {
  case class Texture(val name: String, val width: Int, val height: Int, val channels: Int, val buffer: ByteBuffer)
  private val textures = new HashMap[String, Texture]()
  def get(name: String): Texture = {
    Using (MemoryStack.stackPush()) { stack => 
      val file = new File(f"client/resources/${name}")
      val path = file.getAbsolutePath()
      val pWidth = stack.mallocInt(1)
      val pHeight = stack.mallocInt(1)
      val pChannels = stack.mallocInt(1)
      val buffer = STBImage.stbi_load(path, pWidth, pHeight, pChannels, 4)
      textures.put(name, Texture(name, pWidth.get(0), pHeight.get(0), pChannels.get(0), buffer))
    }
    return textures.get(name)
  }
}
