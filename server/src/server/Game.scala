package server

object Game extends Thread {
  override def run(): Unit = {
    println("Game Running...")
    while (true) {
      Thread.sleep(1000)
      println("Tick")
      Network.broadcast("There has been a tick!")
    }
  }
}