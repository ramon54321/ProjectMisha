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
import scala.util.Using
import org.joml.Matrix4f

object Constants {
  val SCREEN_WIDTH = 1280
  val SCREEN_HEIGHT = 720
}

object Game {

}

object Window {
  def start() = {
    System.out.println(f"Running with LWJGL version ${Version.getVersion}")

    GLFWErrorCallback.createPrint(System.err).set()

    if (!glfwInit())
      throw new IllegalStateException("Unable to initialize GLFW")

    glfwDefaultWindowHints()
    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
    glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE)
    glfwWindowHint(GLFW_SAMPLES, 2)
    glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4)
    glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)
    glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW_TRUE)

    // Create the window
    val window = glfwCreateWindow(
      Constants.SCREEN_WIDTH,
      Constants.SCREEN_HEIGHT,
      "Misha",
      NULL,
      NULL
    )
    if (window == NULL)
      throw new RuntimeException("Failed to create GLFW window")

    // Key Callback
    glfwSetKeyCallback(
      window,
      (window, key, scancode, action, mods) => {
        if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
          glfwSetWindowShouldClose(window, true)
        if (action == GLFW_RELEASE) {
          if (key == GLFW_KEY_Q) {
            println("Pressed Q")
          }
        }
      }
    )

    // Push a new frame onto the thread stack
    Using(stackPush()) { stack =>
      val pWidth = stack.mallocInt(1)
      val pHeight = stack.mallocInt(1)

      // Get window size into pointers
      glfwGetWindowSize(window, pWidth, pHeight)

      // Get primary monitor resolution
      val primaryMonitor = glfwGetPrimaryMonitor()
      val videoMode = glfwGetVideoMode(primaryMonitor)

      // Center window on monitor
      glfwSetWindowPos(
        window,
        (videoMode.width() - pWidth.get(0)) / 2,
        (videoMode.height() - pHeight.get(0)) / 2
      )
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

    // OpenGL Setup
    glEnable(GL_MULTISAMPLE)
    glEnable(GL_CULL_FACE)
    glCullFace(GL_BACK)
    glFrontFace(GL_CCW)
    glEnable(GL_BLEND)
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

    // Game Layer
    val projectionMatrix = new Matrix4f().ortho(
      -Constants.SCREEN_WIDTH / 2,
      Constants.SCREEN_WIDTH / 2,
      -Constants.SCREEN_HEIGHT / 2,
      Constants.SCREEN_HEIGHT / 2,
      -1,
      1
    )
    val staticSpriteBatchRenderer = new StaticSpriteBatchRenderer()
    staticSpriteBatchRenderer.addSprite(new StaticSprite(0f, 0f, 128, 128))

    // Set the clear color
    glClearColor(0.0f, 0.0f, 0.0f, 0.0f)

    // Render loop
    while (!glfwWindowShouldClose(window)) {
      // Clear framebuffer
      glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)

      // Game Layer
      staticSpriteBatchRenderer.render(projectionMatrix, 0, 0)

      // Swap the color buffers
      glfwSwapBuffers(window)

      // Invoke key callback
      glfwPollEvents()
    }
  }
}
