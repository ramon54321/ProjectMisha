package client

import shared.Threading
import client.network.Network
import client.game.Game

object Main {
  def main(args: Array[String]): Unit = {
    if (args.contains("dev")) {} else if (args.contains("benchmark")) {
      val testFlagIndex = args.indexOf("-n")
      if (testFlagIndex < 0 || testFlagIndex + 1 > args.length - 1) return
      val testNames = args(testFlagIndex + 1)
      Benchmark.run(testNames.split(","))
    } else {
      Network.start()
      Game
      Window
      Threading.registerShutdownHook()
    }
  }
}
