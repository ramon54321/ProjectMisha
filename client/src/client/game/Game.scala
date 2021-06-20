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
import client.engine.graphics.TextBatchRenderer
import client.engine.graphics.Window
import client.engine.graphics.Textures
import client.engine.graphics.SpriteSheet

import shared.game.NetworkEvents
import shared.game.NETWORK_EVENT_CREATE_FIXTURE
import shared.engine.IdUtils
import shared.engine.Grid
import shared.game.NETWORK_EVENT_CREATE_ENTITY
import client.engine.graphics.Sprite
import client.engine.graphics.BatchSprite
import client.engine.graphics.BasicBatchRenderer
import client.engine.graphics.BatchRenderManager

class Chunk {
  var isLoaded = false
  val fixturesSprites = new ArrayBuffer[BatchSprite]()
}

object Game {
  Events.on[EVENT_GL_READY](_ => glReady())
  Events.on[EVENT_GL_RENDER](_ => glRender())
  Events.on[EVENT_GL_UPDATE](_ => glUpdate())
  Events.on[EVENT_TICKER_SECOND](_ => tickerSecond())

  // Constants
  private val PIXELS_PER_METER = 32
  
  // Camera
  private var projectionMatrix: Matrix4f = null
  private var cameraX = 0f
  private var cameraY = 0f
  
  // Entity Tracking
  private val activeFixturesSprites = new ArrayBuffer[BatchSprite]
  private val entityIdToSpriteMap = new HashMap[Int, Sprite]

  // Fixture Tracking
  private val activeEntitiesSprites = new ArrayBuffer[BatchSprite]

  // Renderer Manager
  private var batchRenderManager: BatchRenderManager = null

  // Text Renderers
  private val textBatchRenderers = new HashMap[String, TextBatchRenderer]()

  // Load Spritesheet
  private val (resourceErrors, textures) = Array(
    Textures.get("patch1.png"),
    Textures.get("empty.png"),
    Textures.get("grass1.png")
    ).partitionMap(identity)
  resourceErrors.foreach(println)
  private val spriteSheet = SpriteSheet.fromTextures("mainsheet", textures)

  // Chunk Management
  val fixtureSpriteChunks = new Grid[Chunk]
  private def loadChunk(x: Int, y: Int): Unit = {
    for {
      chunk <- fixtureSpriteChunks.getCell(x, y)
    } yield {
      if (!chunk.isLoaded) {
        println(f"Loading chunk $x, $y")
        chunk.fixturesSprites.foreach(sprite => activeFixturesSprites.addOne(sprite))
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
        chunk.fixturesSprites.foreach(sprite => activeFixturesSprites.remove(activeFixturesSprites.indexOf(sprite)))
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

    batchRenderManager = new BatchRenderManager(projectionMatrix)
    val basicBatchRenderer = new BasicBatchRenderer(spriteSheet)

    NetworkEvents.on[NETWORK_EVENT_CREATE_FIXTURE](e => {
      val sprite = new BatchSprite(
        IdUtils.generateId(),
        (e.x * PIXELS_PER_METER).toFloat,
        (e.y * PIXELS_PER_METER).toFloat,
        e.r,
        1,
        e.spriteName,
        basicBatchRenderer,
      )
      val chunkX = (sprite.x / 256).toInt
      val chunkY = (sprite.y / 256).toInt
      val chunk = fixtureSpriteChunks.getCellElseUpdate(chunkX, chunkY, new Chunk())
      chunk.fixturesSprites.addOne(sprite)
      if chunk.isLoaded then activeFixturesSprites.addOne(sprite)
    })

    NetworkEvents.on[NETWORK_EVENT_CREATE_ENTITY](e => {
      for
        entity <- NetworkState.getEntityById(e.id)
        transform <- entity.getComponent("Transform")
        x <- transform.get("x")
        y <- transform.get("y")
      yield
        val sprite = new BatchSprite(
          IdUtils.generateId(),
          x.asInstanceOf[Float] * PIXELS_PER_METER,
          y.asInstanceOf[Float] * PIXELS_PER_METER,
          0,
          1,
          "empty.png",
          basicBatchRenderer,
        )
        entityIdToSpriteMap.put(e.id, sprite)
        activeEntitiesSprites.addOne(sprite)
    })

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
    Benchmark.startTag("batchRenderersSort")
    val spritesToSubmit = activeFixturesSprites.concat(activeEntitiesSprites)
    spritesToSubmit.sortInPlaceWith((a, b) => a.y - b.y > 0)
    Benchmark.endTag("batchRenderersSort")
    Benchmark.startTag("batchRenderersSubmits")
    batchRenderManager.setCameraPosition(cameraX, cameraY)
    spritesToSubmit.foreach(batchRenderManager.submitSprite)
    val batchInfo = batchRenderManager.complete()
    Benchmark.endTag("batchRenderersSubmits")

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
      .map(_.setText(f"Entities: ${activeEntitiesSprites.size}/${NetworkState.getEntities().size}"))
    textBatchRenderers
      .get("fixturesCount")
      .map(_.setText(f"Fixtures: ${activeFixturesSprites.size}/${NetworkState.getFixtures().size}"))

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
