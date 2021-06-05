package client

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import java.lang

import client.game.Game
import client.engine.graphics.Window
import client.engine.NetworkState
import client.engine.Network
import client.engine.Benchmark
import shared.engine.Threading

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
      NetworkState
      Game
      Window
      Threading.registerShutdownHook()
    }
  }
}
