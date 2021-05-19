package client

import scala.util.Random
import scala.collection.mutable.ArrayBuffer

object Benchmark {

  val RUNS = 250
  val N = 100000

  test("ArrayBuffer addAll", () => {
    val positions = Array(
      -0.5f,  0.5f,
      -0.5f, -0.5f,
      0.5f, -0.5f,
      0.5f, 0.5f,
    )
    val positionsAggregate = new ArrayBuffer[Float]()
    for (i <- 0 until N) {
      positionsAggregate.addAll(positions)
    }
  })

  test("Preallocated Array", () => {
    val positions = Array(
      -0.5f,  0.5f,
      -0.5f, -0.5f,
      0.5f, -0.5f,
      0.5f, 0.5f,
    )
    val positionsAggregate = new Array[Float](N * 8)
    for (i <- 0 until N) {
      val offset = i * 8
      for (x <- 0 until 8) {
        positionsAggregate(offset + x) = positions(x)
      }
    }
  })

  private def test(name: String, action: () => Unit) = {
    val times = ArrayBuffer[Double]()
    for (i <- 0 until RUNS) {
      val timeStart = System.nanoTime()
      action()
      val timeElapsed = (System.nanoTime() - timeStart) / 1e9d
      times.addOne(timeElapsed)
      // println("Run " + name + ": " + timeElapsed + "s")
    }
    val averageTime = times.reduce((a, b) => a + b) / times.length
    println(name + ": Avg " + averageTime + "s")
  }
}
