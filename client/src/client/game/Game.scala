package client.game

import client.Events
import client.EventTag._
import client.graphics._
import org.joml.Matrix4f
import client.Constants

object Game {
  Events.on(EVENT_GL_READY, glReady)
  Events.on(EVENT_GL_RENDER, glRender)
  
  var projectionMatrix: Matrix4f = null
  var staticSpriteBatchRenderer: StaticSpriteBatchRenderer = null

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
    staticSpriteBatchRenderer.render(projectionMatrix, 0, 0)
  }
}
