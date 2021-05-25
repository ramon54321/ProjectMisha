package client.graphics

import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL12._
import org.lwjgl.opengl.GL13._
import org.lwjgl.opengl.GL15._
import org.lwjgl.opengl.GL20._
import org.lwjgl.opengl.GL30._
import org.joml._
import util.control.Breaks._
import org.lwjgl.stb.STBTruetype._
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.system.MemoryStack.stackPush
import scala.collection.mutable.TreeSet
import scala.collection.mutable.ArrayBuffer
import scala.util.Using
import org.lwjgl.system.MemoryStack
import client.Benchmark
import client.Window
import org.lwjgl.BufferUtils
import org.lwjgl.stb.STBTTFontinfo
import org.lwjgl.stb.STBTTBakedChar
import org.lwjgl.stb.STBTTAlignedQuad

class TextBatchRenderer(
    private var text: String,
    val position: Vector2fc,
    val color: Vector4fc,
    val fontHeight: Int
) extends BatchRenderer {
  // Length of string
  private val maxChars: Int = 512

  // Keep track to know when to resend buffer data to GPU
  private var isBuffersOutdated = true

  // Generate OpenGL objects' handles
  private val textureHandle = glGenTextures()
  private val vboPositions = glGenBuffers()
  private val vboUvs = glGenBuffers()
  private val vio = glGenBuffers()
  private val vao = glGenVertexArrays()
  private val vertexShader = glCreateShader(GL_VERTEX_SHADER)
  private val fragmentShader = glCreateShader(GL_FRAGMENT_SHADER)
  private val shaderProgram = glCreateProgram()
  private var uniformGlobalColor = 0
  private var uniformModelView = 1
  private var uniformMainTexture = 2

  def setText(text: String) = {
    // Set buffers to update
    isBuffersOutdated = true

    // Set text
    this.text = text
  }

  // Preallocate Mat4s
  private val viewMatrix = new Matrix4f()

  // Declare font handles
  private val bitmapX = 512
  private val bitmapY = 512
  private var charInfo = STBTTBakedChar.malloc(96)
  private var fontAscent = 0
  private var fontDescent = 0
  private var fontLineGap = 0
  private var fontScale = 0f

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

    // Load Font Bytes from Resource
    // TODO: Move font loading out of renderer
    val resourceStreamBytes =
      getClass().getResourceAsStream("/FiraSans-ExtraBold.ttf").readAllBytes()
    val fontByteBuffer = BufferUtils
      .createByteBuffer(resourceStreamBytes.length)
      .put(resourceStreamBytes)
      .flip()

    // Initialize font
    val fontInfo = STBTTFontinfo.create()
    if (!stbtt_InitFont(fontInfo, fontByteBuffer)) println("Couldn't init font")

    // Create bitmap buffer and bake font into bitmap
    val charBitmap = BufferUtils.createByteBuffer(bitmapX * bitmapY)
    stbtt_BakeFontBitmap(
      fontByteBuffer,
      fontHeight,
      charBitmap,
      bitmapX,
      bitmapY,
      32,
      charInfo
    )

    // Get font VMetrics
    Using(stackPush()) { stack =>
      val pAscent = stack.mallocInt(1)
      val pDescent = stack.mallocInt(1)
      val pLineGap = stack.mallocInt(1)
      stbtt_GetFontVMetrics(fontInfo, pAscent, pDescent, pLineGap)
      fontAscent = pAscent.get(0)
      fontDescent = pDescent.get(0)
      fontLineGap = pLineGap.get(0)
    }
    fontScale = stbtt_ScaleForPixelHeight(fontInfo, fontHeight)

    // Send texture data to GPU
    glTexImage2D(
      GL_TEXTURE_2D,
      0,
      GL_RED,
      bitmapX,
      bitmapY,
      0,
      GL_RED,
      GL_UNSIGNED_BYTE,
      charBitmap
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
            |in vec2 vertexUvs;
            |uniform mat4 modelView;
            |out vec2 uvs;
            |void main(void) {
            | uvs = vertexUvs;
            | gl_Position = modelView * vec4(vertexPosition.xy, 0.0, 1.0);
            |}
            |""".stripMargin

    val fragmentShaderSource =
      """
            |#version 400
            |uniform vec4 globalColor;
            |uniform sampler2D mainTexture;
            |in vec2 uvs;
            |out vec4 frag_colour;
            |void main(void) {
            | vec4 texCol = texture(mainTexture, uvs);
            | frag_colour = vec4(texCol.r, texCol.r, texCol.r, texCol.r) * globalColor;
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
    // Create VBOs with enough capacity to accomodate maxChars
    glBindBuffer(GL_ARRAY_BUFFER, vboPositions)
    glBufferData(GL_ARRAY_BUFFER, maxChars * 2 * 4 * 4, GL_DYNAMIC_DRAW)
    glBindBuffer(GL_ARRAY_BUFFER, vboUvs)
    glBufferData(GL_ARRAY_BUFFER, maxChars * 2 * 4 * 4, GL_DYNAMIC_DRAW)
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vio)
    glBufferData(
      GL_ELEMENT_ARRAY_BUFFER,
      maxChars * 6 * 4 * 4,
      GL_DYNAMIC_DRAW
    )

    // Create VAO and setup Vertex Attributes
    glBindVertexArray(vao)
    glBindBuffer(GL_ARRAY_BUFFER, vboPositions)
    glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, NULL)
    glBindBuffer(GL_ARRAY_BUFFER, vboUvs)
    glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, NULL)
    glEnableVertexAttribArray(0)
    glEnableVertexAttribArray(1)
  }
  private def sendBuffers(
      positions: Array[Float],
      uvs: Array[Float],
      indexes: Array[Int]
  ) = {
    // Send data into buffers
    glBindBuffer(GL_ARRAY_BUFFER, vboPositions)
    glBufferSubData(GL_ARRAY_BUFFER, 0, positions)
    glBindBuffer(GL_ARRAY_BUFFER, vboUvs)
    glBufferSubData(GL_ARRAY_BUFFER, 0, uvs)
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vio)
    glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, 0, indexes)
  }
  private val positions = new Array[Float](maxChars * 2 * 4)
  private val uvs = new Array[Float](maxChars * 2 * 4)
  private val indexes = new Array[Int](maxChars * 6 * 4)
  private def updateBuffers() = {
    // Mark buffers as clean
    isBuffersOutdated = false

    // Consecutively add sprites' attributes and indexes into buffers
    Using(stackPush()) { stack =>
      val quad = STBTTAlignedQuad.mallocStack(stack)
      val x = stack.floats(0.0f)
      val y = stack.floats(0.0f)

      val verticalOffset = (fontHeight / 2).toInt

      for (j <- 0 until text.size) {
        breakable {
          val charToDraw = text.charAt(j).toInt

          // Move down a line and move to next char if char is newline
          if (charToDraw == '\n') {
            y.put(
              0,
              y.get(0) + (fontAscent - fontDescent + fontLineGap) * fontScale
            )
            x.put(0, 0.0f)
            break
          }

          stbtt_GetBakedQuad(
            charInfo,
            bitmapX,
            bitmapY,
            charToDraw - 32,
            x,
            y,
            quad,
            true
          )

          val positionOffset = j * 8
          positions(positionOffset + 0) = quad.x0 + position.x()
          positions(positionOffset + 1) =
            -quad.y1 - verticalOffset + position.y()
          positions(positionOffset + 2) = quad.x1 + position.x()
          positions(positionOffset + 3) =
            -quad.y1 - verticalOffset + position.y()
          positions(positionOffset + 4) = quad.x0 + position.x()
          positions(positionOffset + 5) =
            -quad.y0 - verticalOffset + position.y()
          positions(positionOffset + 6) = quad.x1 + position.x()
          positions(positionOffset + 7) =
            -quad.y0 - verticalOffset + position.y()

          val uvOffset = j * 8
          uvs(uvOffset + 0) = quad.s0
          uvs(uvOffset + 1) = quad.t1
          uvs(uvOffset + 2) = quad.s1
          uvs(uvOffset + 3) = quad.t1
          uvs(uvOffset + 4) = quad.s0
          uvs(uvOffset + 5) = quad.t0
          uvs(uvOffset + 6) = quad.s1
          uvs(uvOffset + 7) = quad.t0

          val spriteIndexes = Sprite.indexes.map(i => i + j * 4)
          val indexOffset = j * 6
          for (i <- 0 until 6) {
            indexes(indexOffset + i) = spriteIndexes(i)
          }
        }
      }
    }

    // Send aggregated buffers to GPU
    sendBuffers(positions, uvs, indexes)
  }
  def flush(projectionMatrix: Matrix4fc, cameraX: Float, cameraY: Float) = {
    // Update buffers with sprites' data if needed
    Benchmark.startTag("textBatchRendererUpdateBuffers")
    if (isBuffersOutdated) updateBuffers()
    Benchmark.endTag("textBatchRendererUpdateBuffers")

    // Use batch's shader program
    glUseProgram(shaderProgram)

    // Use batch's VAO
    glBindVertexArray(vao)

    // Use batch's VIO
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vio)

    // Send uniform data to GPU
    glUniform4f(uniformGlobalColor, color.x(), color.y(), color.z(), color.w())

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
    Benchmark.startTag("textBatchRendererDrawElements")
    glDrawElements(GL_TRIANGLES, 6 * text.size, GL_UNSIGNED_INT, 0)
    Benchmark.endTag("textBatchRendererDrawElements")
  }
}
