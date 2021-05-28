package server

import shared.Threading

object Main {
  def main(args: Array[String]) = {
    Network.start()
    ServerNetworkState
    Game
    Ticker.start()
    Threading.registerShutdownHook()
  }
}
