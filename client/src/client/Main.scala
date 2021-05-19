package client

import shared.Threading

object Main {
  def main(args: Array[String]) = {
//    Network.start()
    Window.start()
    Threading.registerShutdownHook()
  }
}
