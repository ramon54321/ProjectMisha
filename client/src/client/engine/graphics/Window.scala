package client.engine.graphics

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

import client.engine.Events
import client.engine.EVENT_GL_READY
import client.engine.EVENT_GL_RENDER
import client.engine.EVENT_GL_UPDATE
import client.engine.EVENT_TICKER_SECOND
import client.Constants

object Window {
  System.out.println(
    f"Running with LWJGL version ${Version.getVersion} on display ${System.getenv("DISPLAY")}"
  )

  GLFWErrorCallback.createPrint(System.err).set()

  if (!glfwInit())
    throw new IllegalStateException("Unable to initialize GLFW")

  glfwDefaultWindowHints()
  glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
  glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE)
  glfwWindowHint(GLFW_SAMPLES, 0)
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
      if (key == GLFW_KEY_E && action == GLFW_REPEAT)
        println("Pressed E")
      if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
        glfwSetWindowShouldClose(window, true)
    }
  )

  def keyDown(key: Int): Boolean = glfwGetKey(window, key) == GLFW_PRESS

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
  Events.emit(EVENT_GL_READY())

  // Set the clear color
  glClearColor(0.0f, 0.0f, 0.0f, 0.0f)

  // Track Frame Times
  private var frameCount = 0
  private var lastFrameCount = 0
  private var lastSecondTime = System.nanoTime()
  private var lastFrameTime = System.nanoTime()
  private var lastFrameDeltaTime: Float = 0
  def deltaTime() = lastFrameDeltaTime
  def fps() = lastFrameCount

  // Start Loop
  while (!glfwWindowShouldClose(window)) {
    // Clear framebuffer
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)

    // Emit Render
    Events.emit(EVENT_GL_RENDER())

    // Swap the color buffers
    glfwSwapBuffers(window)

    // Invoke key callback
    glfwPollEvents()

    // Emit Update
    Events.emit(EVENT_GL_UPDATE())

    // Update Frame Times
    val currentFrameTime = System.nanoTime()
    frameCount += 1
    if (currentFrameTime - lastSecondTime > 1000000000) {
      lastSecondTime = currentFrameTime
      lastFrameCount = frameCount
      Events.emit(EVENT_TICKER_SECOND())
      println("FPS: " + lastFrameCount)
      frameCount = 0
    }
    lastFrameDeltaTime = ((currentFrameTime - lastFrameTime) / 1000000000f)
    lastFrameTime = currentFrameTime
  }
}
