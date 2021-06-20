package client.engine.graphics

import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL12._
import org.lwjgl.opengl.GL13._
import org.lwjgl.opengl.GL15._
import org.lwjgl.opengl.GL20._
import org.lwjgl.opengl.GL30._
import org.joml._
import org.lwjgl.system.MemoryUtil.NULL
import scala.collection.mutable.TreeSet
import scala.collection.mutable.ArrayBuffer
import scala.util.Using
import org.lwjgl.system.MemoryStack

import client.engine.Benchmark
import client.engine.graphics.Texture

class BasicBatchRenderer(override val spriteSheet: SpriteSheet, val maxSprites: Int = 512) extends BatchRenderer(spriteSheet) {

  // Generate OpenGL objects' handles
  private val textureHandle = glGenTextures()
  private val vboPositions = glGenBuffers()
  private val vboColors = glGenBuffers()
  private val vboUvs = glGenBuffers()
  private val vio = glGenBuffers()
  private val vao = glGenVertexArrays()
  private val vertexShader = glCreateShader(GL_VERTEX_SHADER)
  private val fragmentShader = glCreateShader(GL_FRAGMENT_SHADER)
  private val shaderProgram = glCreateProgram()
  private var uniformGlobalColor = 0
  private var uniformModelView = 1
  private var uniformMainTexture = 2

  // Preallocate Mat4s
  private val viewMatrix = new Matrix4f()

  // Setup batch object
  setupTexture()
  setupShader()
  setupBuffers()

  private def setupTexture() = {
    // Set texture parameters
    glBindTexture(GL_TEXTURE_2D, textureHandle)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)

    // Send texture data to GPU
    glTexImage2D(
      GL_TEXTURE_2D,
      0,
      GL_RGBA,
      spriteSheet.texture.width,
      spriteSheet.texture.height,
      0,
      GL_RGBA,
      GL_UNSIGNED_BYTE,
      spriteSheet.texture.buffer
    )

    // Generate mipmap
    glGenerateMipmap(GL_TEXTURE_2D)
  }

  private def setupShader() = {
    // Specify shaders' source
    val vertexShaderSource =
      """
            |#version 400
            |in vec2 vertexPosition;
            |in vec3 vertexColor;
            |in vec2 vertexUvs;
            |uniform mat4 modelView;
            |out vec3 color;
            |out vec2 uvs;
            |void main(void) {
            | color = vertexColor;
            | uvs = vertexUvs;
            | gl_Position = modelView * vec4(vertexPosition.xy, 0.0, 1.0);
            |}
            |""".stripMargin

    val fragmentShaderSource =
      """
            |#version 400
            |uniform vec4 globalColor;
            |uniform sampler2D mainTexture;
            |in vec3 color;
            |in vec2 uvs;
            |out vec4 frag_colour;
            |void main(void) {
            | //frag_colour = vec4(globalColor.xyz * color, 1.0);
            | frag_colour = texture(mainTexture, uvs);
            |}
            |""".stripMargin

    // Send shaders' source to GPU
    glShaderSource(vertexShader, vertexShaderSource)
    glShaderSource(fragmentShader, fragmentShaderSource)

    // Compile shaders
    glCompileShader(vertexShader)
    glCompileShader(fragmentShader)

    // Validate shaders' compilation status
    Validate.shader(vertexShader)
    Validate.shader(fragmentShader)

    // Attach shaders to shader program
    glAttachShader(shaderProgram, vertexShader)
    glAttachShader(shaderProgram, fragmentShader)

    // Link shader program
    glLinkProgram(shaderProgram)

    // Bind VAO Before Validation (Mac Specific)
    glBindVertexArray(vao)

    // Validate shader program
    glValidateProgram(shaderProgram)
    Validate.program(shaderProgram)

    // Get locations for uniforms in shader program
    uniformGlobalColor = glGetUniformLocation(shaderProgram, "globalColor")
    uniformModelView = glGetUniformLocation(shaderProgram, "modelView")
    uniformMainTexture = glGetUniformLocation(shaderProgram, "mainTexture")
  }

  private def setupBuffers() = {
    // Create VBOs with enough capacity to accomodate maxSprites
    glBindBuffer(GL_ARRAY_BUFFER, vboPositions)
    glBufferData(GL_ARRAY_BUFFER, maxSprites * 2 * 4 * 4, GL_DYNAMIC_DRAW)
    glBindBuffer(GL_ARRAY_BUFFER, vboColors)
    glBufferData(GL_ARRAY_BUFFER, maxSprites * 3 * 4 * 4, GL_DYNAMIC_DRAW)
    glBindBuffer(GL_ARRAY_BUFFER, vboUvs)
    glBufferData(GL_ARRAY_BUFFER, maxSprites * 2 * 4 * 4, GL_DYNAMIC_DRAW)
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vio)
    glBufferData(
      GL_ELEMENT_ARRAY_BUFFER,
      maxSprites * 6 * 4 * 4,
      GL_DYNAMIC_DRAW
    )

    // Create VAO and setup Vertex Attributes
    val attribLocationPosition = glGetAttribLocation(shaderProgram, "vertexPosition")
    val attribLocationColor = glGetAttribLocation(shaderProgram, "vertexColor")
    val attribLocationUvs = glGetAttribLocation(shaderProgram, "vertexUvs")
    glBindVertexArray(vao)
    glBindBuffer(GL_ARRAY_BUFFER, vboPositions)
    glVertexAttribPointer(attribLocationPosition, 2, GL_FLOAT, false, 0, NULL)
    glBindBuffer(GL_ARRAY_BUFFER, vboColors)
    glVertexAttribPointer(attribLocationColor, 3, GL_FLOAT, false, 0, NULL)
    glBindBuffer(GL_ARRAY_BUFFER, vboUvs)
    glVertexAttribPointer(attribLocationUvs, 2, GL_FLOAT, false, 0, NULL)
    glEnableVertexAttribArray(attribLocationPosition)
    glEnableVertexAttribArray(attribLocationColor)
    glEnableVertexAttribArray(attribLocationUvs)
  }

  private def sendBuffers(
      positions: Array[Float],
      colors: Array[Float],
      uvs: Array[Float],
      indexes: Array[Int]
  ) = {
    // Send data into buffers
    glBindBuffer(GL_ARRAY_BUFFER, vboPositions)
    glBufferSubData(GL_ARRAY_BUFFER, 0, positions)
    glBindBuffer(GL_ARRAY_BUFFER, vboColors)
    glBufferSubData(GL_ARRAY_BUFFER, 0, colors)
    glBindBuffer(GL_ARRAY_BUFFER, vboUvs)
    glBufferSubData(GL_ARRAY_BUFFER, 0, uvs)
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vio)
    glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, 0, indexes)
  }

  private val positions = new Array[Float](maxSprites * 2 * 4)
  private val colors = new Array[Float](maxSprites * 3 * 4)
  private val uvs = new Array[Float](maxSprites * 2 * 4)
  private val indexes = new Array[Int](maxSprites * 6 * 4)
  private var batchSpriteCount = 0

  def isFull(): Boolean = batchSpriteCount >= maxSprites

  def submitSprite(sprite: BatchSprite): Unit =
    val spritePositions = sprite.getPositions()
    val positionOffset = batchSpriteCount * 8
    for (i <- 0 until 8) {
      // Add sprite's position to each base vertex position
      if (i % 2 == 0) {
        positions(positionOffset + i) = spritePositions(i) + sprite.x
      } else {
        positions(positionOffset + i) = spritePositions(i) + sprite.y
      }
    }

    // Add sprite colors to colors collection
    val spriteColors = sprite.getColors()
    val colorOffset = batchSpriteCount * 12
    for (i <- 0 until 12) {
      colors(colorOffset + i) = spriteColors(i)
    }

    // Add sprite uvs to uvs collection
    val spriteUvs = sprite.getUvs()
    val uvOffset = batchSpriteCount * 8
    for (i <- 0 until 8) {
      uvs(uvOffset + i) = spriteUvs(i)
    }

    // Add sprite indexes to indexes collection with offset
    val spriteIndexes = sprite.getIndexes().map(i => i + batchSpriteCount * 4)
    val indexOffset = batchSpriteCount * 6
    for (i <- 0 until 6) {
      indexes(indexOffset + i) = spriteIndexes(i)
    }

    batchSpriteCount += 1
  end submitSprite

  def flush(projectionMatrix: Matrix4fc, cameraX: Float, cameraY: Float) = {
    // Send Current Batch
    sendBuffers(positions, colors, uvs, indexes)

    // Use batch's shader program
    glUseProgram(shaderProgram)

    // Use batch's VAO
    glBindVertexArray(vao)

    // Use batch's VIO
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vio)

    // Send uniform data to GPU
    glUniform4f(uniformGlobalColor, 0.0f, 1.0f, 1.0f, 1.0f)

    // Send camera position to GPU
    Using(MemoryStack.stackPush()) { stack =>
      projectionMatrix.translate(-cameraX, -cameraY, 0f, viewMatrix)
      glUniformMatrix4fv(
        uniformModelView,
        false,
        viewMatrix.get(stack.mallocFloat(16))
      )
    }

    // Use batch's texture
    glBindTexture(GL_TEXTURE_2D, textureHandle)
    glUniform1i(uniformMainTexture, 0)

    // Submit draw call to GPU
    glDrawElements(GL_TRIANGLES, 6 * batchSpriteCount, GL_UNSIGNED_INT, 0)

    // Reset Batch
    batchSpriteCount = 0
  }
}
