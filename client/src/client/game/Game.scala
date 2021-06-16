package client.game

import org.lwjgl.glfw.GLFW._
import org.joml.Matrix4f
import org.joml.Vector4f
import org.joml.Vector2f
import scala.util.Random
import scala.collection.mutable.HashMap
import scala.util.Try
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import concurrent.ExecutionContext.Implicits.global

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

import shared.game.NetworkEvents
import shared.game.NETWORK_EVENT_CREATE_FIXTURE
import shared.engine.IdUtils
import shared.engine.Grid

class Chunk {
  var isLoaded = false
  val sprites = new ArrayBuffer[StaticSprite]()
}

object Game {
  Events.on[EVENT_GL_READY](_ => glReady())
  Events.on[EVENT_GL_RENDER](_ => glRender())
  Events.on[EVENT_GL_UPDATE](_ => glUpdate())
  Events.on[EVENT_TICKER_SECOND](_ => tickerSecond())

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

  val fixtureSpriteChunks = new Grid[Chunk]
  private def loadChunk(x: Int, y: Int): Unit = {
    for {
      chunk <- fixtureSpriteChunks.getCell(x, y)
    } yield {
      if (!chunk.isLoaded) {
        println(f"Loading chunk $x, $y")
        chunk.sprites.foreach(sprite => baseBatchRenderer.addSprite(sprite))
        chunk.isLoaded = true
      }
    }
  }
  private def unloadChunk(x: Int, y: Int): Unit = {
    for {
      chunk <- fixtureSpriteChunks.getCell(x, y)
    } yield {
      if (chunk.isLoaded) {
        println(f"Unloading chunk $x, $y")
        chunk.sprites.foreach(sprite => baseBatchRenderer.removeSprite(sprite))
        chunk.isLoaded = false
      }
    }
  }

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

    NetworkEvents.on[NETWORK_EVENT_CREATE_FIXTURE](e => {
      val sprite = new StaticSprite(
        IdUtils.generateId(),
        e.x * 64f,
        e.y * 64f,
        Random.nextFloat() * org.joml.Math.PI.toFloat * 2,
        1,
        spriteSheet,
        "patch1.png"
      )
      val chunkX = (sprite.x / 256).toInt
      val chunkY = (sprite.y / 256).toInt
      val chunk = fixtureSpriteChunks.getCellElseUpdate(chunkX, chunkY, new Chunk())
      chunk.sprites.addOne(sprite)
      baseBatchRenderer.addSprite(sprite)
    })

    debugBatchRenderer = new StaticSpriteBatchRenderer(spriteSheet.texture, 4)
    debugBatchRenderer.addSprite(
      new StaticSprite(0, 0, 0, 0, 1, spriteSheet, "empty.png")
    )

    baseBatchRenderer = new StaticSpriteBatchRenderer(spriteSheet.texture, 8192)

    noidBatchRenderer =
      new DynamicSpriteBatchRenderer(spriteSheet.texture, 8192)
    for (i <- 0 until 64) {
      noidBatchRenderer.addSprite(
        new StaticSprite(
          i,
          -600 + Random.nextFloat() * 1200,
          -400 + Random.nextFloat() * 800,
          Random.nextFloat() * org.joml.Math.PI.toFloat * 2,
          1,
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
    Benchmark.startTag("debugBatchRendererFlush")
    debugBatchRenderer.flush(projectionMatrix, cameraX, cameraY)
    Benchmark.endTag("debugBatchRendererFlush")
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

    // Update Chunks
    val cameraChunkX = (cameraX / 256).toInt
    val cameraChunkY = (cameraY / 256).toInt
    val cameraChunkXMin = cameraChunkX - 1
    val cameraChunkXMax = cameraChunkX + 1
    val cameraChunkYMin = cameraChunkY - 1
    val cameraChunkYMax = cameraChunkY + 1
    for
      x <- cameraChunkXMin - 1 to cameraChunkXMax + 1
      y <- cameraChunkYMin - 1 to cameraChunkYMax + 1
    yield
      if (x == cameraChunkXMin - 1 || x == cameraChunkXMax + 1 || y == cameraChunkYMin - 1 || y == cameraChunkYMax + 1)
      then unloadChunk(x, y)
      else loadChunk(x, y)
  }

  private def tickerSecond(): Unit = {
    textBatchRenderers.get("fps").map(_.setText("FPS: " + Window.fps()))

    val entities = NetworkState.getEntities()
    val fixtures = NetworkState.getFixtures()

    for (
      entity <- Try(entities.last).toOption;
      component <- entity.getComponent("Health");
      health <- component.get("Health")
    ) yield {
      println(health)
    }
  }
}
