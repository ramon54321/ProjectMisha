package server

import server.network.Network
import server.game.Game
import server.networkstate.NetworkState
import server.ticker.Ticker
import shared.Threading

object Main {
  def main(args: Array[String]): Unit = {
    Network.start()
    NetworkState
    Game
    Ticker.start()
    Threading.registerShutdownHook()
  }
}
