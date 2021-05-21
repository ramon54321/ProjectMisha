package client.graphics

import org.lwjgl.opengl.GL20._

object Validate {
  private val MESSAGE_LENGTH = 500
  def shader(shader: Int) = {
    val message = glGetShaderInfoLog(shader, MESSAGE_LENGTH)
    if (message.length > 0) println(message)
  }
  def program(program: Int) = {
    val message = glGetProgramInfoLog(program, MESSAGE_LENGTH)
    if (message.length > 0) println(message)
  }
}
