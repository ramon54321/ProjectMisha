package server

import shared.Threading

object Main {
  def main(args: Array[String]) = {
    Network.start()
    Game.start()
    Threading.registerShutdownHook()
  }
}
