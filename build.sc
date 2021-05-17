import mill._, scalalib._

trait CommonModule extends ScalaModule {
  def scalaVersion = "2.13.5"
}

object client extends CommonModule {
  def moduleDeps = Seq(shared)
  def ivyDeps = Agg(
    ivy"org.lwjgl:lwjgl:3.2.3",
    ivy"org.lwjgl:lwjgl-assimp:3.2.3",
    ivy"org.lwjgl:lwjgl-glfw:3.2.3",
    ivy"org.lwjgl:lwjgl-openal:3.2.3",
    ivy"org.lwjgl:lwjgl-opengl:3.2.3",
    ivy"org.lwjgl:lwjgl-stb:3.2.3",
    ivy"org.lwjgl:lwjgl:natives-macos",
    ivy"org.lwjgl:lwjgl-assimp:natives-macos",
    ivy"org.lwjgl:lwjgl-glfw:natives-macos",
    ivy"org.lwjgl:lwjgl-openal:natives-macos",
    ivy"org.lwjgl:lwjgl-opengl:natives-macos",
    ivy"org.lwjgl:lwjgl-stb:natives-macos",
    ivy"org.joml:joml:1.10.1",
  )
}

object server extends CommonModule {
  def moduleDeps = Seq(shared)
}

object shared extends CommonModule {}
