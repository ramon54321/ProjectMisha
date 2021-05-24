package client.game

import client.Events
import client.EventTag._
import client.graphics._
import org.joml.Matrix4f
import client.Constants
import client.Window
import org.lwjgl.glfw.GLFW._
import scala.util.Random
import scala.collection.mutable.ArrayBuffer
import client.Benchmark

object Game {
  Events.on(EVENT_GL_READY, glReady)
  Events.on(EVENT_GL_RENDER, glRender)
  Events.on(EVENT_GL_UPDATE, glUpdate)

  var projectionMatrix: Matrix4f = null
  var baseBatchRenderer: StaticSpriteBatchRenderer = null

  var cameraX = 0f
  var cameraY = 0f

  private val baseStaticSprites = new ArrayBuffer[StaticSprite]()

  private def glReady() = {
    projectionMatrix = new Matrix4f().ortho(
      -Constants.SCREEN_WIDTH / 2,
      Constants.SCREEN_WIDTH / 2,
      -Constants.SCREEN_HEIGHT / 2,
      Constants.SCREEN_HEIGHT / 2,
      -1,
      1
    )

    baseBatchRenderer = new StaticSpriteBatchRenderer(8192)
    for (i <- 0 until 4096) {
      baseBatchRenderer.addSprite(
        new StaticSprite(
          i,
          -600 + Random.nextFloat() * 1200,
          -400 + Random.nextFloat() * 800,
          32,
          32
        )
      )
    }
  }

  private def glRender() = {
    Benchmark.startTag("baseBatchRendererFlush")
    baseBatchRenderer.flush(projectionMatrix, cameraX, cameraY)
    Benchmark.endTag("baseBatchRendererFlush")
  }

  private def glUpdate() = {
    val deltaTime = Window.deltaTime()
    if (Window.keyDown(GLFW_KEY_A)) {
      cameraX -= 400 * deltaTime
    }
    if (Window.keyDown(GLFW_KEY_D)) {
      cameraX += 400 * deltaTime
    }
    if (Window.keyDown(GLFW_KEY_S)) {
      cameraY -= 400 * deltaTime
    }
    if (Window.keyDown(GLFW_KEY_W)) {
      cameraY += 400 * deltaTime
    }
  }
}
