package server

import shared.Threading

object Main {
  def main(args: Array[String]) = {
    Server
    Threading.registerShutdownHook()

    Thread.sleep(5000)

    Server.broadcast("Hello, world!")
  }
}
