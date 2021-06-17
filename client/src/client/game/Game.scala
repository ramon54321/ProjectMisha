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
import shared.game.NETWORK_EVENT_CREATE_ENTITY
import client.engine.graphics.Sprite

class Chunk {
  var isLoaded = false
  val sprites = new ArrayBuffer[StaticSprite]()
}

object Game {
  Events.on[EVENT_GL_READY](_ => glReady())
  Events.on[EVENT_GL_RENDER](_ => glRender())
  Events.on[EVENT_GL_UPDATE](_ => glUpdate())
  Events.on[EVENT_TICKER_SECOND](_ => tickerSecond())

  val PIXELS_PER_METER = 32

  var projectionMatrix: Matrix4f = null
  var debugBatchRenderer: StaticSpriteBatchRenderer = null
  var fixtureBatchRenderer: StaticSpriteBatchRenderer = null
  var entityBatchRenderer: DynamicSpriteBatchRenderer = null

  private val textBatchRenderers = new HashMap[String, TextBatchRenderer]()

  val (resourceErrors, textures) = Array(
    Textures.get("patch1.png"),
    Textures.get("empty.png"),
    Textures.get("grass1.png")
  ).partitionMap(identity)
  resourceErrors.foreach(println)
  private val spriteSheet = SpriteSheet.fromTextures("mainsheet", textures)

  var cameraX = 0f
  var cameraY = 0f

  val fixtureSpriteChunks = new Grid[Chunk]
  private def loadChunk(x: Int, y: Int): Unit = {
    for {
      chunk <- fixtureSpriteChunks.getCell(x, y)
    } yield {
      if (!chunk.isLoaded) {
        println(f"Loading chunk $x, $y")
        chunk.sprites.foreach(sprite => fixtureBatchRenderer.addSprite(sprite))
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
        chunk.sprites.foreach(sprite => fixtureBatchRenderer.removeSprite(sprite))
        chunk.isLoaded = false
      }
    }
  }

  val entityIdToSpriteMap = new HashMap[Int, Sprite]

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
        (e.x * PIXELS_PER_METER).toFloat,
        (e.y * PIXELS_PER_METER).toFloat,
        e.r,
        1,
        spriteSheet,
        e.spriteName,
      )
      val chunkX = (sprite.x / 256).toInt
      val chunkY = (sprite.y / 256).toInt
      val chunk = fixtureSpriteChunks.getCellElseUpdate(chunkX, chunkY, new Chunk())
      chunk.sprites.addOne(sprite)
      if chunk.isLoaded then fixtureBatchRenderer.addSprite(sprite)
    })

    NetworkEvents.on[NETWORK_EVENT_CREATE_ENTITY](e => {
      for
        entity <- NetworkState.getEntityById(e.id)
        transform <- entity.getComponent("Transform")
        x <- transform.get("x")
        y <- transform.get("y")
      yield
        val sprite = new Sprite(
          IdUtils.generateId(),
          x.asInstanceOf[Float] * PIXELS_PER_METER,
          y.asInstanceOf[Float] * PIXELS_PER_METER,
          0,
          1,
          spriteSheet,
          "empty.png",
        )
        entityIdToSpriteMap.put(e.id, sprite)
        entityBatchRenderer.addSprite(sprite)
    })

    debugBatchRenderer = new StaticSpriteBatchRenderer(spriteSheet.texture, 4)
    // debugBatchRenderer.addSprite(
    //   new StaticSprite(0, 0, 0, 0, 1, spriteSheet, "empty.png")
    // )

    fixtureBatchRenderer = new StaticSpriteBatchRenderer(spriteSheet.texture, 8192)
    entityBatchRenderer = new DynamicSpriteBatchRenderer(spriteSheet.texture, 8192)

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
    Benchmark.startTag("fixtureBatchRendererFlush")
    fixtureBatchRenderer.flush(projectionMatrix, cameraX, cameraY)
    Benchmark.endTag("fixtureBatchRendererFlush")
    Benchmark.startTag("entityBatchRendererFlush")
    entityBatchRenderer.flush(projectionMatrix, cameraX, cameraY)
    Benchmark.endTag("entityBatchRendererFlush")
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
    // if (!networkMessages.isEmpty) println(networkMessages.mkString(" "))
    networkMessages.foreach(NetworkState.applyPatch)

    // Respond to network state events
    NetworkState.flushEvents()

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
      .map(_.setText(f"Entities: ${entityBatchRenderer.getSpriteCount()}/${NetworkState.getEntities().size}"))
    textBatchRenderers
      .get("fixturesCount")
      .map(_.setText(f"Fixtures: ${fixtureBatchRenderer.getSpriteCount()}/${NetworkState.getFixtures().size}"))

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

    // Update Entities
    for
      entity <- NetworkState.getEntities()
      transform <- entity.getComponent("Transform")
      x <- transform.get("x")
      y <- transform.get("y")
      sprite <- entityIdToSpriteMap.get(entity.id)
    yield
      sprite.x = x.asInstanceOf[Float] * PIXELS_PER_METER
      sprite.y = y.asInstanceOf[Float] * PIXELS_PER_METER
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
