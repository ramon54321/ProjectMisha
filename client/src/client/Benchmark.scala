package client

import scala.util.Random
import scala.collection.mutable.ArrayBuffer
import scala.util.Sorting

object Benchmark {

  val RUNS = 250
  val N = 100000

  test("QuickSort Array Ints", (start, end) => {
    val elements = Array.fill(N){Random.nextInt(N / 2)}
    start()
    Sorting.quickSort(elements)
    end()
  })

  test("QuickSort Array Objects", (start, end) => {
    case class Person(age: Int)
    implicit val personOrdering = new Ordering[Person]() {
      override def compare(x: Person, y: Person): Int = {
        return x.age - y.age
      }
    }
    val elements = Array.fill(N){new Person(Random.nextInt(N / 2))}
    start()
    Sorting.quickSort(elements)
    end()
  })

  test("SortInPlace Array Objects", (start, end) => {
    case class Person(age: Int)
    implicit val personOrdering = new Ordering[Person]() {
      override def compare(x: Person, y: Person): Int = {
        return x.age - y.age
      }
    }
    val elements = Array.fill(N){new Person(Random.nextInt(N / 2))}
    start()
    elements.sortInPlace()
    end()
  })

  test("SortWith Array Objects", (start, end) => {
    case class Person(age: Int)
    val elements = Array.fill(N){new Person(Random.nextInt(N / 2))}
    start()
    elements.sortWith((a, b) => a.age < b.age)
    end()
  })

  test("ArrayBuffer addAll", (start, end) => {
    val positions = Array(
      -0.5f,  0.5f,
      -0.5f, -0.5f,
      0.5f, -0.5f,
      0.5f, 0.5f,
    )
    val positionsAggregate = new ArrayBuffer[Float]()
    start()
    for (i <- 0 until N) {
      positionsAggregate.addAll(positions)
    }
    end()
  })

  test("Preallocated Array", (start, end) => {
    val positions = Array(
      -0.5f,  0.5f,
      -0.5f, -0.5f,
      0.5f, -0.5f,
      0.5f, 0.5f,
    )
    val positionsAggregate = new Array[Float](N * 8)
    start()
    for (i <- 0 until N) {
      val offset = i * 8
      for (x <- 0 until 8) {
        positionsAggregate(offset + x) = positions(x)
      }
    }
    end()
  })

  private def test(name: String, action: (() => Unit, () => Unit) => Unit) = {
    val times = ArrayBuffer[Double]()
    for (i <- 0 until RUNS) {
      var timeStart = 0L
      var timeElapsed = 0.0
      action(() => timeStart = System.nanoTime(), () => timeElapsed = (System.nanoTime() - timeStart) / 1e9d)
      times.addOne(timeElapsed)
    }
    val averageTime = times.reduce((a, b) => a + b) / times.length
    println(name + ": Avg " + averageTime + "s")
  }
}
