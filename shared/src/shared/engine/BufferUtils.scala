package shared.engine

import java.nio.ByteBuffer

object BufferUtils {
  def byteBufferToHexString(buffer: ByteBuffer, wordBytes: Int): String = {
    val _buffer = buffer.duplicate
    val array = new Array[Byte](_buffer.remaining)
    _buffer.get(array)
    val byteHexStrings = array.map(b => String.format("%02X", b))
    val wordGroups = byteHexStrings.grouped(wordBytes)
    return wordGroups.map(group => group.mkString(" ")).mkString("\n")
  }
}
