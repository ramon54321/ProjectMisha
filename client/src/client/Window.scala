package client

import org.lwjgl.Version
import org.lwjgl.glfw.GLFW._
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL.createCapabilities
import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL13._
import org.lwjgl.opengl.GL15._
import org.lwjgl.opengl.GL20._
import org.lwjgl.opengl.GL30._
import org.lwjgl.opengl.GLUtil
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.NULL
import scala.collection.mutable.ArrayBuffer
import scala.util.Using

object Window {
  def start() = {
    System.out.println(f"Running with LWJGL version ${Version.getVersion}")

    GLFWErrorCallback.createPrint(System.err).set()

    if (!glfwInit())
      throw new IllegalStateException("Unable to initialize GLFW")

    glfwDefaultWindowHints()
    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
    glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE)
    glfwWindowHint(GLFW_SAMPLES, 2)
    glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4)
    glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)
    glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW_TRUE)

    // Create the window
    val window = glfwCreateWindow(800, 800, "Misha", NULL, NULL)
    if (window == NULL) throw new RuntimeException("Failed to create GLFW window")

    // Key Callback
    glfwSetKeyCallback(window, (window, key, scancode, action, mods) => {
      if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) glfwSetWindowShouldClose(window, true)
      if (action == GLFW_RELEASE) {
        if (key == GLFW_KEY_Q) {
          println("Pressed Q")
        }
      }
    })

    // Push a new frame onto the thread stack
    Using (stackPush()) { stack => 
      val pWidth = stack.mallocInt(1)
      val pHeight = stack.mallocInt(1)

      // Get window size into pointers
      glfwGetWindowSize(window, pWidth, pHeight)

      // Get primary monitor resolution
      val primaryMonitor = glfwGetPrimaryMonitor()
      val videoMode = glfwGetVideoMode(primaryMonitor)

      // Center window on monitor
      glfwSetWindowPos(window, (videoMode.width() - pWidth.get(0)) / 2, (videoMode.height() - pHeight.get(0)) / 2)
    }

    // Make OpenGL context current
    glfwMakeContextCurrent(window)

    // Set GSync
    glfwSwapInterval(1)

    // Set window visible
    glfwShowWindow(window)

    // Create OpenGL Context
    val glCapabilities = createCapabilities()
    GLUtil.setupDebugMessageCallback(System.out)

    // GL Setup
    glEnable(GL_MULTISAMPLE)
    glEnable(GL_CULL_FACE)
    glCullFace(GL_BACK)
    glFrontFace(GL_CCW)
    glEnable(GL_BLEND)
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

    // Test
    class Sprite(var x: Float = 0, var y: Float = 0) {
      def getPositions(): Array[Float] = Sprite.positions
      def getColors(): Array[Float] = Sprite.colors
      def getUvs(): Array[Float] = Sprite.uvs
      def getIndexes(): Array[Int] = Sprite.indexes
    }
    object Sprite {
      val positions = Array(
        -0.5f,  0.5f,
        -0.5f, -0.5f,
        0.5f, -0.5f,
        0.5f, 0.5f,
      )
      val colors = Array(
        0.5f, 0.5f, 0.0f,
        0.5f, 0.5f, 0.5f,
        0.5f, 0.5f, 0.8f,
        0.5f, 0.0f, 0.5f,
      )
      val uvs = Array(
        0.0f, 1.0f,
        0.0f, 0.0f,
        1.0f, 0.0f,
        1.0f, 1.0f,
      )
      val indexes = Array[Int](
        0, 1, 2, 2, 3, 0
      )
    }

    object Batch {
      private var spriteCount = 0
      val positions = new ArrayBuffer[Float]()
      val colors = new ArrayBuffer[Float]()
      val uvs = new ArrayBuffer[Float]()
      val indexes = new ArrayBuffer[Int]()
      def addSprite(sprite: Sprite) = {
        positions.addAll(sprite.getPositions())
        val offset = spriteCount * 8
        for (i <- 0 until 8) {
          if (i % 2 == 0) {
            positions(offset + i) += sprite.x
          } else {
            positions(offset + i) += sprite.y
          }
        }
        colors.addAll(sprite.getColors())
        uvs.addAll(sprite.getUvs())
        indexes.addAll(sprite.getIndexes().map(i => i + spriteCount * 4))
        spriteCount += 1
      }
      val textureHandle = glGenTextures()
      val vboPositions = glGenBuffers()
      val vboColors = glGenBuffers()
      val vboUvs = glGenBuffers()
      val vio = glGenBuffers()
      val vao = glGenVertexArrays()
      val vertexShader = glCreateShader(GL_VERTEX_SHADER)
      val fragmentShader = glCreateShader(GL_FRAGMENT_SHADER)
      val shaderProgram = glCreateProgram()
      var uniformGlobalColor = 0
      var uniformMainTexture = 1
      private def buildTexture() = {
        glBindTexture(GL_TEXTURE_2D, textureHandle)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 2, 2, 0, GL_RGBA, GL_FLOAT, Array(
          1.0f, 0.0f, 0.0f, 0.5f,
          1.0f, 1.0f, 1.0f, 0.5f,
          1.0f, 1.0f, 1.0f, 0.5f,
          1.0f, 1.0f, 1.0f, 0.9f,
        ))
        glGenerateMipmap(GL_TEXTURE_2D)
      }
      private def buildShader() = {
        val vertexShaderSource =
          """
            |#version 400
            |in vec2 vertexPosition;
            |in vec3 vertexColor;
            |in vec2 vertexUvs;
            |out vec3 color;
            |out vec2 uvs;
            |void main(void) {
            | color = vertexColor;
            | uvs = vertexUvs;
            | gl_Position = vec4(vertexPosition, 1.0, 1.0);
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

        glShaderSource(vertexShader, vertexShaderSource)
        glShaderSource(fragmentShader, fragmentShaderSource)
        glCompileShader(vertexShader)
        glCompileShader(fragmentShader)
        Validate.shader(vertexShader)
        Validate.shader(fragmentShader)

        glAttachShader(shaderProgram, vertexShader)
        glAttachShader(shaderProgram, fragmentShader)
        glLinkProgram(shaderProgram)
        glValidateProgram(shaderProgram)
        Validate.program(shaderProgram)

        uniformGlobalColor = glGetUniformLocation(shaderProgram, "globalColor")
        uniformMainTexture = glGetUniformLocation(shaderProgram, "mainTexture")
      }
      def buildBuffers() = {
        glBindBuffer(GL_ARRAY_BUFFER, vboPositions)
        glBufferData(GL_ARRAY_BUFFER, positions.length * 4, GL_STATIC_DRAW)
        glBindBuffer(GL_ARRAY_BUFFER, vboColors)
        glBufferData(GL_ARRAY_BUFFER, colors.length * 4, GL_STATIC_DRAW)
        glBindBuffer(GL_ARRAY_BUFFER, vboUvs)
        glBufferData(GL_ARRAY_BUFFER, uvs.length * 4, GL_STATIC_DRAW)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vio)
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexes.length * 4, GL_STATIC_DRAW)
        
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
      def build() = {
        buildTexture()
        buildShader()
        buildBuffers()
      }
      def updateBuffers() = {
        glBindBuffer(GL_ARRAY_BUFFER, vboPositions)
        glBufferSubData(GL_ARRAY_BUFFER, 0, positions.toArray)
        glBindBuffer(GL_ARRAY_BUFFER, vboColors)
        glBufferSubData(GL_ARRAY_BUFFER, 0, colors.toArray)
        glBindBuffer(GL_ARRAY_BUFFER, vboUvs)
        glBufferSubData(GL_ARRAY_BUFFER, 0, uvs.toArray)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vio)
        glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, 0, indexes.toArray)
      }
      def render() = {
        glUseProgram(shaderProgram)
        glUniform4f(uniformGlobalColor, 0.0f, 1.0f, 1.0f, 1.0f)
        glBindTexture(GL_TEXTURE_2D, textureHandle)
        glBindVertexArray(vao)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vio)
        glDrawElements(GL_TRIANGLES, 6 * spriteCount, GL_UNSIGNED_INT, 0)
      }
    }

    Batch.addSprite(new Sprite(0f, 0f))
    Batch.addSprite(new Sprite(-0.6f, 0.0f))
    Batch.addSprite(new Sprite(0.3f, 0.2f))
    Batch.build()
    Batch.updateBuffers()
    
    // Set the clear color
    glClearColor(0.0f, 0.0f, 0.0f, 0.0f)

    // Render loop
    while (!glfwWindowShouldClose(window)) {
      // Clear framebuffer
      glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)

      // Test
      Batch.render()

      // Swap the color buffers
      glfwSwapBuffers(window)

      // Invoke key callback
      glfwPollEvents()
    }
  }
}
