package server

import server.engine.network.Network
import server.engine.NetworkState
import server.engine.Ticker
import shared.engine.Threading
import server.game.Game

object Main {
  def main(args: Array[String]): Unit = {
    Network.start()
    NetworkState
    Game
    Ticker.start()
    Threading.registerShutdownHook()
  }
}
