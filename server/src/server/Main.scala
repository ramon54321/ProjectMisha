package server

import shared.Threading
import shared.NetworkState

object Main {
  def main(args: Array[String]) = {
    Network.start()
    NetworkState.asWriter()
    Game.start()
    Threading.registerShutdownHook()
  }
}
