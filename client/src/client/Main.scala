package client

import shared.Threading
import client.network.Network
import client.game.Game
import scala.collection.mutable.ArrayBuffer
import shared.NetworkState
import scala.collection.mutable.HashMap
import java.lang

object Main {
  var args: Array[String] = null
  def main(args: Array[String]): Unit = {
    this.args = args
    if (args.contains("dev")) {} else if (args.contains("benchmark")) {
      val testFlagIndex = args.indexOf("-n")
      if (testFlagIndex < 0 || testFlagIndex + 1 > args.length - 1) return
      val testNames = args(testFlagIndex + 1)
      Benchmark.run(testNames.split(","))
    } else {
      Network.start()
      NetworkState.asReader()
      Game
      Window
      Threading.registerShutdownHook()
    }
  }
}
