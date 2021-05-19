package client

import org.lwjgl.Version
import org.lwjgl.glfw.GLFW._
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL.createCapabilities
import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL15._
import org.lwjgl.opengl.GL20._
import org.lwjgl.opengl.GL30._
import org.lwjgl.opengl.GLUtil
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.NULL

object Window {
  def start() = {
    System.out.println(f"Running with LWJGL version ${Version.getVersion}")

    GLFWErrorCallback.createPrint(System.err).set()

    if (!glfwInit())
      throw new IllegalStateException("Unable to initialize GLFW")

    glfwDefaultWindowHints()
    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
    glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE)
    glfwWindowHint(GLFW_SAMPLES, 4)
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
    try {
      val stack = stackPush()
      val pWidth = stack.mallocInt(1)
      val pHeight = stack.mallocInt(1)

      // Get window size into pointers
      glfwGetWindowSize(window, pWidth, pHeight)

      // Get primary monitor resolution
      val primaryMonitor = glfwGetPrimaryMonitor()
      val videoMode = glfwGetVideoMode(primaryMonitor)

      // Center window on monitor
      glfwSetWindowPos(window, (videoMode.width() - pWidth.get(0)) / 2, (videoMode.height() - pHeight.get(0)) / 2)
    } catch {
      case e: Exception => println("Error pushing frame onto thread stack")
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
    glEnable(GL_CULL_FACE)
    glCullFace(GL_BACK)
    glFrontFace(GL_CCW)

    // Test
    val vertexData = Array(
      0.0f,  0.5f,  0.0f, 
      -0.5f, -0.5f,  0.0f,
      0.5f, -0.5f,  0.0f,
    )
    val vbo = glGenBuffers()
    glBindBuffer(GL_ARRAY_BUFFER, vbo)
    glBufferData(GL_ARRAY_BUFFER, vertexData, GL_STATIC_DRAW)

    val vao = glGenVertexArrays()
    glBindVertexArray(vao)
    glEnableVertexAttribArray(0)
    glBindBuffer(GL_ARRAY_BUFFER, vbo)
    glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, NULL)

    val vertexShaderSource =
      """
        |#version 400
        |in vec3 vertexPosition;
        |void main(void) {
        | gl_Position = vec4(vertexPosition, 1.0);
        |}
        |""".stripMargin

    val fragmentShaderSource =
      """
        |#version 400
        |uniform vec4 globalColor;
        |out vec4 frag_colour;
        |void main(void) {
        | frag_colour = globalColor;
        |}
        |""".stripMargin

    val vertexShader = glCreateShader(GL_VERTEX_SHADER)
    val fragmentShader = glCreateShader(GL_FRAGMENT_SHADER)
    glShaderSource(vertexShader, vertexShaderSource)
    glShaderSource(fragmentShader, fragmentShaderSource)
    glCompileShader(vertexShader)
    glCompileShader(fragmentShader)
    Validate.shader(vertexShader)
    Validate.shader(fragmentShader)

    val shaderProgram = glCreateProgram()
    glAttachShader(shaderProgram, vertexShader)
    glAttachShader(shaderProgram, fragmentShader)
    glLinkProgram(shaderProgram)
    glValidateProgram(shaderProgram)
    Validate.program(shaderProgram)

    val uniformGlobalColor = glGetUniformLocation(shaderProgram, "globalColor")

    // Set the clear color
    glClearColor(0.0f, 0.0f, 0.0f, 0.0f)

    // Render loop
    while (!glfwWindowShouldClose(window)) {
      // Clear framebuffer
      glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)

      // Test
      glUseProgram(shaderProgram)
      glUniform4f(uniformGlobalColor, 0.0f, 1.0f, 1.0f, 1.0f)
      glBindVertexArray(vao)
      glDrawArrays(GL_TRIANGLES, 0, 3)

      // Swap the color buffers
      glfwSwapBuffers(window)

      // Invoke key callback
      glfwPollEvents()
    }
  }
}
