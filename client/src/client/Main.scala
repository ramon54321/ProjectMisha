package client

import shared.Threading

object Main {
  def main(args: Array[String]): Unit = {
    if (args.contains("benchmark")) {
      val testFlagIndex = args.indexOf("-n")
      if (testFlagIndex < 0 || testFlagIndex + 1 > args.length - 1) return
      val testNames = args(testFlagIndex + 1)
      Benchmark.run(testNames.split(","))
    } else {
      Network.start()
      Window.start()
      Threading.registerShutdownHook()
    }
  }
}
