package client.game

import org.lwjgl.glfw.GLFW._
import org.joml.Matrix4f
import org.joml.Vector4f
import org.joml.Vector2f
import scala.util.Random
import scala.collection.mutable.HashMap

import client.Constants
import client.network.Network
import client.networkstate.NetworkState
import client.events.Events
import client.events.EventTag.EVENT_GL_READY
import client.events.EventTag.EVENT_GL_RENDER
import client.events.EventTag.EVENT_GL_UPDATE
import client.events.EventTag.EVENT_TICKER_SECOND
import client.graphics.StaticSpriteBatchRenderer
import client.graphics.DynamicSpriteBatchRenderer
import client.graphics.TextBatchRenderer
import client.graphics.StaticSprite
import client.graphics.Window
import client.Benchmark
import scala.util.Try

object Game {
  Events.on(EVENT_GL_READY, () => glReady())
  Events.on(EVENT_GL_RENDER, () => glRender())
  Events.on(EVENT_GL_UPDATE, () => glUpdate())
  Events.on(EVENT_TICKER_SECOND, () => tickerSecond())

  NetworkState.Events.on("setWorldName", args => println("Hook: " + args))

  var projectionMatrix: Matrix4f = null
  var baseBatchRenderer: StaticSpriteBatchRenderer = null
  var noidBatchRenderer: DynamicSpriteBatchRenderer = null

  private val textBatchRenderers = new HashMap[String, TextBatchRenderer]()

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

    baseBatchRenderer = new StaticSpriteBatchRenderer(8192)
    for (i <- 0 until 2) {
      baseBatchRenderer.addSprite(
        new StaticSprite(
          i,
          -600 + Random.nextFloat() * 1200,
          -400 + Random.nextFloat() * 800,
          128,
          128
        )
      )
    }

    noidBatchRenderer = new DynamicSpriteBatchRenderer(8192)
    for (i <- 0 until 4096) {
      noidBatchRenderer.addSprite(
        new StaticSprite(
          i,
          -600 + Random.nextFloat() * 1200,
          -400 + Random.nextFloat() * 800,
          32,
          32
        )
      )
    }

    textBatchRenderers.put(
      "fps",
      new TextBatchRenderer(
        "---",
        new Vector2f(
          -Constants.SCREEN_WIDTH / 2 + 16,
          Constants.SCREEN_HEIGHT / 2 - 16 - 24 * 0
        ),
        new Vector4f(0.533f, 0.866f, 0.274f, 1.0f),
        18
      )
    )
    textBatchRenderers.put(
      "version",
      new TextBatchRenderer(
        Constants.VERSION,
        new Vector2f(
          -Constants.SCREEN_WIDTH / 2 + 16,
          Constants.SCREEN_HEIGHT / 2 - 16 - 24 * 1
        ),
        new Vector4f(0.533f, 0.866f, 0.274f, 1.0f),
        18
      )
    )
    textBatchRenderers.put(
      "entityCount",
      new TextBatchRenderer(
        f"Entities: ${NetworkState.getEntities().size}",
        new Vector2f(
          -Constants.SCREEN_WIDTH / 2 + 16,
          Constants.SCREEN_HEIGHT / 2 - 16 - 24 * 2
        ),
        new Vector4f(0.533f, 0.866f, 0.274f, 1.0f),
        18
      )
    )
  }

  private def glRender() = {
    Benchmark.startTag("baseBatchRendererFlush")
    baseBatchRenderer.flush(projectionMatrix, cameraX, cameraY)
    Benchmark.endTag("baseBatchRendererFlush")
    Benchmark.startTag("noidBatchRendererFlush")
    noidBatchRenderer.flush(projectionMatrix, cameraX, cameraY)
    Benchmark.endTag("noidBatchRendererFlush")
    Benchmark.startTag("textBatchRenderersFlush")
    textBatchRenderers.valuesIterator.foreach(textBatchRenderer =>
      textBatchRenderer.flush(projectionMatrix, 0, 0)
    )
    Benchmark.endTag("textBatchRenderersFlush")
  }

  private def glUpdate() = {
    val deltaTime = Window.deltaTime()

    // Handle Server Messages
    val networkMessages = Network.dequeueMessages()
    if (!networkMessages.isEmpty) println(networkMessages.mkString(" "))
    networkMessages.foreach(NetworkState.applyPatch)

    // Keyboard Input
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
    if (Window.keyDown(GLFW_KEY_P)) {
      textBatchRenderers.get("fps").map(_.setText("It worked!"))
    }

    // Update UI
    textBatchRenderers
      .get("entityCount")
      .map(_.setText(f"Entities: ${NetworkState.getEntities().size}"))
  }

  private def tickerSecond() = {
    textBatchRenderers.get("fps").map(_.setText("FPS: " + Window.fps()))

    val entities = NetworkState.getEntities()
    entities.foreach(e => println(e.getComponents()))

    for (
      entity <- Try(entities.last).toOption;
      component <- entity.getComponent("Health");
      health <- component.get("Health")
    ) yield {
      println(health)
    }
  }
}
