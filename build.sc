import mill._, scalalib._

trait CommonModule extends ScalaModule {
  def scalaVersion = "2.13.5"
}

object client extends CommonModule {
  
  val isMac = System.getProperty("os.name").toLowerCase().contains("mac")

  def moduleDeps = Seq(shared)
  def ivyDeps = Agg(
    ivy"org.lwjgl:lwjgl:3.2.3",
    ivy"org.lwjgl:lwjgl-assimp:3.2.3",
    ivy"org.lwjgl:lwjgl-glfw:3.2.3",
    ivy"org.lwjgl:lwjgl-openal:3.2.3",
    ivy"org.lwjgl:lwjgl-opengl:3.2.3",
    ivy"org.lwjgl:lwjgl-cuda:3.2.3",
    ivy"org.lwjgl:lwjgl-nuklear:3.2.3",
    ivy"org.lwjgl:lwjgl-stb:3.2.3",
    ivy"org.joml:joml:1.10.1",
  )
  def unmanagedClasspath = T {
    if (!os.exists(millSourcePath / os.up / "natives")) Agg()
    else Agg.from(os.list(millSourcePath / os.up / "natives")).map(PathRef(_))
  }
  def forkArgs = if (isMac) Seq("-XstartOnFirstThread") else Seq[String]()
}

object server extends CommonModule {
  def moduleDeps = Seq(shared)
}

object shared extends CommonModule {}
