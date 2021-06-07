package client.engine.graphics

import scala.collection.immutable.HashMap

class SpriteSheet(
    val texture: Texture,
    val meta: HashMap[String, SpriteInfo]
) {}
