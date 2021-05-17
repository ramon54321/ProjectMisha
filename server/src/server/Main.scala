package server

import shared.Threading

object Main {
  def main(args: Array[String]) = {
    val server = new Server()
    server.start()
    Threading.registerShutdownHook()
  }
}
