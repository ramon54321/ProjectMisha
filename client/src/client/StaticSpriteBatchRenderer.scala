package client

import org.lwjgl.opengl.GL11._
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

class StaticSprite(val x: Float = 0, val y: Float = 0, val width: Float = 32, val height: Float = 32) {
  // Create local copy of positions scaled to the local size of the sprite
  private val positions = StaticSprite.positions.clone()
  for (i <- 0 until 8) {
    if (i % 2 == 0) {
      positions(i) *= width
    } else {
      positions(i) *= height
    }
  }
  def getPositions(): Array[Float] = this.positions
  def getColors(): Array[Float] = StaticSprite.colors
  def getUvs(): Array[Float] = StaticSprite.uvs
  def getIndexes(): Array[Int] = StaticSprite.indexes
}
object StaticSprite {
  val positions = Array(
    -0.5f, 0.5f, -0.5f, -0.5f, 0.5f, -0.5f, 0.5f, 0.5f
  )
  val colors = Array(
    0.5f, 0.5f, 0.0f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.8f, 0.5f, 0.0f, 0.5f
  )
  val uvs = Array(
    0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 1.0f
  )
  val indexes = Array[Int](
    0, 1, 2, 2, 3, 0
  )
}

class StaticSpriteBatchRenderer {
  // Maximum number of sprites which can be batched
  private val MAX_SPRITES = 10000

  // Sorted tree of sprites to render in a single batch
  private val sprites = new TreeSet[StaticSprite]()(new Ordering[StaticSprite] {
    // Define sort to draw highest 'y' first (From background to foreground)
    override def compare(x: StaticSprite, y: StaticSprite): Int =
      if (x.y - y.y > 0) -1 else 1
  })

  // Keep track to know when to resend buffer data to GPU
  private var isBuffersOutdated = true

  // Add sprite to batch if possible
  def addSprite(sprite: StaticSprite): Boolean = {
    // Do not add sprite if batch is full
    if (sprites.size >= MAX_SPRITES) return false

    // Mark buffers as dirty to ensure buffer data resend to GPU on next render
    isBuffersOutdated = true

    // Add sprite to render batch
    sprites.add(sprite)
    return true
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
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT)
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
    // Create VBOs with enough capacity to accomodate MAX_SPRITES
    glBindBuffer(GL_ARRAY_BUFFER, vboPositions)
    glBufferData(GL_ARRAY_BUFFER, MAX_SPRITES * 2 * 4, GL_STATIC_DRAW)
    glBindBuffer(GL_ARRAY_BUFFER, vboColors)
    glBufferData(GL_ARRAY_BUFFER, MAX_SPRITES * 3 * 4, GL_STATIC_DRAW)
    glBindBuffer(GL_ARRAY_BUFFER, vboUvs)
    glBufferData(GL_ARRAY_BUFFER, MAX_SPRITES * 2 * 4, GL_STATIC_DRAW)
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vio)
    glBufferData(GL_ELEMENT_ARRAY_BUFFER, MAX_SPRITES * 6 * 4, GL_STATIC_DRAW)

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
  private def updateBuffers() = {
    // Mark buffers as clean
    isBuffersOutdated = false

    // Create temporary collections for buffers
    // TODO: Preallocate arrays instead of arraybuffers
    val positions = new ArrayBuffer[Float]()
    val colors = new ArrayBuffer[Float]()
    val uvs = new ArrayBuffer[Float]()
    val indexes = new ArrayBuffer[Int]()

    // Consecutively add sprites' attributes and indexes into buffers
    var spriteIndex = 0
    sprites.foreach(sprite => {
      // Add sprite positions to positions collection with sprite's own position offset
      positions.addAll(sprite.getPositions())
      val offset = spriteIndex * 8
      for (i <- 0 until 8) {
        // Add sprite's position to each base vertex position
        if (i % 2 == 0) {
          positions(offset + i) += sprite.x
        } else {
          positions(offset + i) += sprite.y
        }
      }
      // Add sprite colors to colors collection
      colors.addAll(sprite.getColors())

      // Add sprite uvs to uvs collection
      uvs.addAll(sprite.getUvs())

      // Add sprite indexes to indexes collection with offset
      indexes.addAll(sprite.getIndexes().map(i => i + spriteIndex * 4))

      spriteIndex += 1
    })

    // Send aggregated buffers to GPU
    sendBuffers(positions.toArray, colors.toArray, uvs.toArray, indexes.toArray)
  }
  def render(projectionMatrix: Matrix4fc, cameraX: Float, cameraY: Float) = {
    // Update buffers with sprites' data if needed
    if (isBuffersOutdated) updateBuffers()

    // Use batch's shader program
    glUseProgram(shaderProgram)

    // Use batch's VAO
    glBindVertexArray(vao)

    // Use batch's VIO
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vio)

    // Send uniform data to GPU
    glUniform4f(uniformGlobalColor, 0.0f, 1.0f, 1.0f, 1.0f)

    // Send camera position to GPU
    Using (MemoryStack.stackPush()) { stack =>
        projectionMatrix.translate(-cameraX, -cameraY, 0f, viewMatrix)
        glUniformMatrix4fv(uniformModelView, false, viewMatrix.get(stack.mallocFloat(16)))
    }

    // Use batch's texture
    glBindTexture(GL_TEXTURE_2D, textureHandle)

    // Submit draw call to GPU
    glDrawElements(GL_TRIANGLES, 6 * sprites.size, GL_UNSIGNED_INT, 0)
  }
}
