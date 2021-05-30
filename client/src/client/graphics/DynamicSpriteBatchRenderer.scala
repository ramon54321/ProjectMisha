package client.graphics

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

import client.Benchmark

class DynamicSpriteBatchRenderer(val maxSprites: Int = 512) extends BatchRenderer {
  // Keep track of sprites in this batch
  private val sprites = new ArrayBuffer[StaticSprite]()

  // Keep track to know when to resend buffer data to GPU
  private var isBuffersOutdated = true

  // Add sprite to batch if possible
  def addSprite(sprite: StaticSprite): Boolean = {
    // Do not add sprite if batch is full
    if (sprites.size >= maxSprites) return false

    // Mark buffers as dirty to ensure buffer data resend to GPU on next render
    isBuffersOutdated = true

    // Add sprite to render batch
    sprites.addOne(sprite)
    return true
  }

  // Remove sprite from batch
  def removeSprite(spriteId: Int): Unit = {
    // Mark buffers as dirty to ensure buffer data resend to GPU on next render
    isBuffersOutdated = true

    // Get sprite index
    val index = sprites.indexWhere(s => s.id == spriteId)

    // Remove sprite from render batch
    sprites.remove(index)
  }
  def removeSprite(sprite: StaticSprite): Unit = {
    removeSprite(sprite.id)
  }

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
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)

    // Send texture data to GPU
    glTexImage2D(
      GL_TEXTURE_2D,
      0,
      GL_RGBA,
      2,
      2,
      0,
      GL_RGBA,
      GL_FLOAT,
      Array(
        1.0f, 0.0f, 0.0f, 0.9f, 1.0f, 1.0f, 1.0f, 0.9f, 1.0f, 1.0f, 1.0f, 0.9f,
        1.0f, 1.0f, 1.0f, 0.9f
      )
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
    glBindVertexArray(vao)
    glBindBuffer(GL_ARRAY_BUFFER, vboPositions)
    glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, NULL)
    glBindBuffer(GL_ARRAY_BUFFER, vboColors)
    glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, NULL)
    glBindBuffer(GL_ARRAY_BUFFER, vboUvs)
    glVertexAttribPointer(2, 2, GL_FLOAT, false, 0, NULL)
    glEnableVertexAttribArray(0)
    glEnableVertexAttribArray(1)
    glEnableVertexAttribArray(2)
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
  private def updateBuffers() = {
    // Mark buffers as clean
    isBuffersOutdated = false

    // Sort sprites
    sprites.sortInPlaceWith((a, b) => a.y - b.y < 0)

    // Consecutively add sprites' attributes and indexes into buffers
    var spriteIndex = 0
    sprites.foreach(sprite => {
      // Add sprite positions to positions collection with sprite's own position offset
      val spritePositions = sprite.getPositions()
      val positionOffset = spriteIndex * 8
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
      val colorOffset = spriteIndex * 12
      for (i <- 0 until 12) {
        colors(colorOffset + i) = spriteColors(i)
      }

      // Add sprite uvs to uvs collection
      val spriteUvs = sprite.getUvs()
      val uvOffset = spriteIndex * 8
      for (i <- 0 until 8) {
        uvs(uvOffset + i) = spriteUvs(i)
      }

      // Add sprite indexes to indexes collection with offset
      val spriteIndexes = sprite.getIndexes().map(i => i + spriteIndex * 4)
      val indexOffset = spriteIndex * 6
      for (i <- 0 until 6) {
        indexes(indexOffset + i) = spriteIndexes(i)
      }

      spriteIndex += 1
    })

    // Send aggregated buffers to GPU
    sendBuffers(positions, colors, uvs, indexes)
  }
  def flush(projectionMatrix: Matrix4fc, cameraX: Float, cameraY: Float) = {
    // Update buffers with sprites' data if needed
    Benchmark.startTag("dynamicBatchRendererUpdateBuffers")
    // if (isBuffersOutdated) updateBuffers()
    updateBuffers()
    Benchmark.endTag("dynamicBatchRendererUpdateBuffers")

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

    // Submit draw call to GPU
    Benchmark.startTag("dynamicBatchRendererDrawElements")
    glDrawElements(GL_TRIANGLES, 6 * sprites.size, GL_UNSIGNED_INT, 0)
    Benchmark.endTag("dynamicBatchRendererDrawElements")
  }
}
