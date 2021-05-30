package server

import server.network.Network
import server.game.Game
import server.networkstate.ServerNetworkState
import server.ticker.Ticker
import shared.Threading

object Main {
  def main(args: Array[String]): Unit = {
    Network.start()
    ServerNetworkState
    Game
    Ticker.start()
    Threading.registerShutdownHook()
  }
}
