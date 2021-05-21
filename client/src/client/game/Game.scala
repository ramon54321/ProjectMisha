package client.game

import client.Events
import client.EventTag._
import client.graphics._
import org.joml.Matrix4f
import client.Constants
import client.Window
import org.lwjgl.glfw.GLFW._

object Game {
  Events.on(EVENT_GL_READY, glReady)
  Events.on(EVENT_GL_RENDER, glRender)
  Events.on(EVENT_GL_UPDATE, glUpdate)

  var projectionMatrix: Matrix4f = null
  var staticSpriteBatchRenderer: StaticSpriteBatchRenderer = null

  var cameraX = 0f
  var cameraY = 0f

  private def glReady() = {
    projectionMatrix = new Matrix4f().ortho(
      -Constants.SCREEN_WIDTH / 2,
      Constants.SCREEN_WIDTH / 2,
      -Constants.SCREEN_HEIGHT / 2,
      Constants.SCREEN_HEIGHT / 2,
      -1,
      1
    )
    staticSpriteBatchRenderer = new StaticSpriteBatchRenderer()
    staticSpriteBatchRenderer.addSprite(new StaticSprite(0f, 0f, 128, 128))
  }

  private def glRender() = {
    staticSpriteBatchRenderer.render(projectionMatrix, cameraX, cameraY)
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
