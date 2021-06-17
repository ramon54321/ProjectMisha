package client.engine

import org.lwjgl.BufferUtils
import java.nio.ByteBuffer

sealed trait ResourceError
case class NotFound(path: String) extends ResourceError

object Resources:
  def load(path: String): Either[ResourceError, ByteBuffer] = 
    if getClass.getResource(path) != null
    then
      val resourceStreamBytes = getClass().getResourceAsStream(path).readAllBytes()
      val resourceByteBuffer = BufferUtils
        .createByteBuffer(resourceStreamBytes.length)
        .put(resourceStreamBytes)
        .flip()
      Right(resourceByteBuffer)
    else
      Left(NotFound(path))
  end load
end Resources
