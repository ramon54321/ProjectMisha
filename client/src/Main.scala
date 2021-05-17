package client

import shared.Threading

object Main {
  def connectToNetwork(): Network = {
    val network = new Network()
    network.start()
    return network
  }

  def main(args: Array[String]) = {
    this.connectToNetwork()
    Window
    Threading.registerShutdownHook()
  }
}
