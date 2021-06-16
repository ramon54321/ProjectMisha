package client.game

import org.lwjgl.glfw.GLFW._
import org.joml.Matrix4f
import org.joml.Vector4f
import org.joml.Vector2f
import scala.util.Random
import scala.collection.mutable.HashMap
import scala.util.Try

import client.Constants
import client.engine.Network
import client.engine.NetworkState
import client.engine.Events
import client.engine.EVENT_GL_READY
import client.engine.EVENT_GL_RENDER
import client.engine.EVENT_GL_UPDATE
import client.engine.EVENT_TICKER_SECOND
import client.engine.Benchmark
import client.engine.graphics.StaticSpriteBatchRenderer
import client.engine.graphics.DynamicSpriteBatchRenderer
import client.engine.graphics.TextBatchRenderer
import client.engine.graphics.StaticSprite
import client.engine.graphics.Window
import client.engine.graphics.Textures
import client.engine.graphics.SpriteSheet

object Game {
  Events.on[EVENT_GL_READY](_ => glReady())
  Events.on[EVENT_GL_RENDER](_ => glRender())
  Events.on[EVENT_GL_UPDATE](_ => glUpdate())
  Events.on[EVENT_TICKER_SECOND](_ => tickerSecond())

  // NetworkState.Events.on("setWorldName", args => println("Hook: " + args))

  var projectionMatrix: Matrix4f = null
  var debugBatchRenderer: StaticSpriteBatchRenderer = null
  var baseBatchRenderer: StaticSpriteBatchRenderer = null
  var noidBatchRenderer: DynamicSpriteBatchRenderer = null

  private val textBatchRenderers = new HashMap[String, TextBatchRenderer]()

  private val spriteSheet = SpriteSheet.fromTextures(
    "mainsheet",
    Array(Textures.get("patch1.png"), Textures.get("empty.png"))
  )

  var cameraX = 0f
  var cameraY = 0f

  private def glReady(): Unit = {
    val halfWidth = Constants.SCREEN_WIDTH.toFloat / 2
    val halfHeight = Constants.SCREEN_HEIGHT.toFloat / 2
    projectionMatrix = new Matrix4f().ortho(
      -halfWidth,
      halfWidth,
      -halfHeight,
      halfHeight,
      -1,
      1
    )

    debugBatchRenderer = new StaticSpriteBatchRenderer(spriteSheet.texture, 4)
    debugBatchRenderer.addSprite(
      new StaticSprite(0, 0, 0, 0, spriteSheet, "empty.png")
    )

    baseBatchRenderer = new StaticSpriteBatchRenderer(spriteSheet.texture, 8192)
    for (i <- 0 until 128) {
      baseBatchRenderer.addSprite(
        new StaticSprite(
          i,
          -600 + Random.nextFloat() * 1200,
          -400 + Random.nextFloat() * 800,
          Random.nextFloat() * org.joml.Math.PI.toFloat * 2,
          spriteSheet,
          "patch1.png"
        )
      )
    }

    noidBatchRenderer =
      new DynamicSpriteBatchRenderer(spriteSheet.texture, 8192)
    for (i <- 0 until 128) {
      noidBatchRenderer.addSprite(
        new StaticSprite(
          i,
          -600 + Random.nextFloat() * 1200,
          -400 + Random.nextFloat() * 800,
          Random.nextFloat() * org.joml.Math.PI.toFloat * 2,
          spriteSheet,
          "empty.png"
        )
      )
    }

    textBatchRenderers.put(
      "fps",
      new TextBatchRenderer(
        "---",
        new Vector2f(
          -halfWidth + 16,
          halfHeight - 16 - 24 * 0
        ),
        new Vector4f(0.533f, 0.866f, 0.274f, 1.0f),
        18f
      )
    )
    textBatchRenderers.put(
      "version",
      new TextBatchRenderer(
        Constants.VERSION,
        new Vector2f(
          -halfWidth + 16,
          halfHeight - 16 - 24 * 1
        ),
        new Vector4f(0.533f, 0.866f, 0.274f, 1.0f),
        18f
      )
    )
    textBatchRenderers.put(
      "entityCount",
      new TextBatchRenderer(
        f"Entities: ${NetworkState.getEntities().size}",
        new Vector2f(
          -halfWidth + 16,
          halfHeight - 16 - 24 * 2
        ),
        new Vector4f(0.533f, 0.866f, 0.274f, 1.0f),
        18f
      )
    )
    textBatchRenderers.put(
      "fixturesCount",
      new TextBatchRenderer(
        f"Fixtures: ${NetworkState.getFixtures().size}",
        new Vector2f(
          -halfWidth + 16,
          halfHeight - 16 - 24 * 3
        ),
        new Vector4f(0.533f, 0.866f, 0.274f, 1.0f),
        18f
      )
    )
  }

  private def glRender(): Unit = {
    Benchmark.startTag("debugBatchRendererFlush")
    debugBatchRenderer.flush(projectionMatrix, cameraX, cameraY)
    Benchmark.endTag("debugBatchRendererFlush")
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

  private def glUpdate(): Unit = {
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
    textBatchRenderers
      .get("fixturesCount")
      .map(_.setText(f"Fixtures: ${NetworkState.getFixtures().size}"))
  }

  private def tickerSecond(): Unit = {
    textBatchRenderers.get("fps").map(_.setText("FPS: " + Window.fps()))

    val entities = NetworkState.getEntities()
    entities.foreach(e => println(e.getComponents()))

    val fixtures = NetworkState.getFixtures()
    fixtures.foreach(f => println(f))

    for (
      entity <- Try(entities.last).toOption;
      component <- entity.getComponent("Health");
      health <- component.get("Health")
    ) yield {
      println(health)
    }
  }
}
