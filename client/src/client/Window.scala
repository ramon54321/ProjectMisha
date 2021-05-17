package client

import org.lwjgl.glfw.GLFW._
import org.lwjgl.glfw.GLFWErrorCallback

object Window {
  def start() = {
    GLFWErrorCallback.createPrint(System.err).set()

    if (!glfwInit())
      throw new IllegalStateException("Unable to initialize GLFW")

    glfwDefaultWindowHints()
    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
    glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE)

    // Create the window
    val window = glfwCreateWindow(300, 300, "Misha", null, null)
    if (window == null) throw new RuntimeException("Failed to create GLFW window")

    // Key Callback
    glfwSetKeyCallback(window, (window, key, scancode, action, mods) => {
      if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) glfwSetWindowShouldClose(window, true)
    })

    // // Get the thread stack and push a new frame
    // try (MemoryStack stack = stackPush()) {
    //   IntBuffer pWidth = stack.mallocInt(1); // int*
    //   IntBuffer pHeight = stack.mallocInt(1); // int*

    //   // Get the window size passed to glfwCreateWindow
    //   glfwGetWindowSize(window, pWidth, pHeight);

    //   // Get the resolution of the primary monitor
    //   GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

    //   // Center the window
    //   glfwSetWindowPos(
    //     window,
    //     (vidmode.width() - pWidth.get(0)) / 2,
    //     (vidmode.height() - pHeight.get(0)) / 2
    //   );
    // } // the stack frame is popped automatically

    // // Make the OpenGL context current
    // glfwMakeContextCurrent(window);
    // // Enable v-sync
    // glfwSwapInterval(1);

    // // Make the window visible
    // glfwShowWindow(window);
  }

  def loop() = {}
}
