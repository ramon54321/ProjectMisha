package client

import scala.util.Random
import scala.collection.mutable.ArrayBuffer
import scala.util.Sorting
import org.joml.Matrix4f
import scala.collection.mutable.HashMap

object Benchmark {

  val RUNS = 250
  val N = 100000

  val tests = new HashMap[String, () => Unit]()

  def run(names: Array[String]) = names.foreach(name => tests.get(name.toLowerCase()).map(test => test()))

  tests.addAll(
    Array(
      (
        "CreateMat4".toLowerCase(),
        () =>
          test(
            "Create Mat4",
            (start, end) => {
              start()
              val elements = Array.fill(N) { new Matrix4f() }
              end()
            }
          )
      ),
      (
        "SetIdentityMat4".toLowerCase(),
        () =>
          test(
            "Set Identity Mat4",
            (start, end) => {
              val elements = Array.fill(N) { new Matrix4f() }
              start()
              elements.foreach(element => element.identity())
              end()
            }
          )
      ),
      (
        "TranslateMat4".toLowerCase(),
        () =>
          test(
            "Translate Mat4",
            (start, end) => {
              val elements = Array.fill(N) { new Matrix4f().identity() }
              val xs = Array.fill(N) { -500 + Random.nextFloat() * 1000f }
              val ys = Array.fill(N) { -500 + Random.nextFloat() * 1000f }
              val zs = Array.fill(N) { -500 + Random.nextFloat() * 1000f }
              start()
              for (i <- 0 until N) {
                elements(i).translate(xs(i), ys(i), zs(i))
              }
              end()
            }
          )
      ),
      (
        "RotateMat4".toLowerCase(),
        () =>
          test(
            "Rotate Mat4",
            (start, end) => {
              val elements = Array.fill(N) { new Matrix4f().identity() }
              val as = Array.fill(N) { -45f + Random.nextFloat() * 90f }
              val xs = Array.fill(N) { -0.5f + Random.nextFloat() * 1.0f }
              val ys = Array.fill(N) { -0.5f + Random.nextFloat() * 1.0f }
              val zs = Array.fill(N) { -0.5f + Random.nextFloat() * 1.0f }
              start()
              for (i <- 0 until N) {
                elements(i).rotate(as(i), xs(i), ys(i), zs(i))
              }
              end()
            }
          )
      ),
      (
        "SetOrthoMat4".toLowerCase(),
        () =>
          test(
            "Set Ortho Mat4",
            (start, end) => {
              val elements = Array.fill(N) { new Matrix4f() }
              val ls = Array.fill(N) { -1000f + Random.nextFloat() * 2000.0f }
              val rs = Array.fill(N) { -1000f + Random.nextFloat() * 2000.0f }
              val bs = Array.fill(N) { -1000f + Random.nextFloat() * 2000.0f }
              val ts = Array.fill(N) { -1000f + Random.nextFloat() * 2000.0f }
              val ns = Array.fill(N) { -45f + Random.nextFloat() * 90f }
              val fs = Array.fill(N) { -45f + Random.nextFloat() * 90f }
              start()
              for (i <- 0 until N) {
                elements(i).ortho(ls(i), rs(i), bs(i), ts(i), ns(i), fs(i)) 
              }
              end()
            }
          )
      ),
      (
        "QuickSortArrayInts".toLowerCase(),
        () =>
          test(
            "QuickSort Array Ints",
            (start, end) => {
              val elements = Array.fill(N) { Random.nextInt(N / 2) }
              start()
              Sorting.quickSort(elements)
              end()
            }
          )
      ),
      (
        "QuickSortArrayObjects".toLowerCase(),
        () =>
          test(
            "QuickSort Array Objects",
            (start, end) => {
              case class Person(age: Int)
              implicit val personOrdering = new Ordering[Person]() {
                override def compare(x: Person, y: Person): Int = {
                  return x.age - y.age
                }
              }
              val elements = Array.fill(N) { new Person(Random.nextInt(N / 2)) }
              start()
              Sorting.quickSort(elements)
              end()
            }
          )
      ),
      (
        "SortInPlaceArrayObjects".toLowerCase(),
        () =>
          test(
            "SortInPlace Array Objects",
            (start, end) => {
              case class Person(age: Int)
              implicit val personOrdering = new Ordering[Person]() {
                override def compare(x: Person, y: Person): Int = {
                  return x.age - y.age
                }
              }
              val elements = Array.fill(N) { new Person(Random.nextInt(N / 2)) }
              start()
              elements.sortInPlace()
              end()
            }
          )
      ),
      (
        "SortWithArrayObjects".toLowerCase(),
        () =>
          test(
            "SortWith Array Objects",
            (start, end) => {
              case class Person(age: Int)
              val elements = Array.fill(N) { new Person(Random.nextInt(N / 2)) }
              start()
              elements.sortWith((a, b) => a.age < b.age)
              end()
            }
          )
      ),
      (
        "ArrayBufferAddAll".toLowerCase(),
        () =>
          test(
            "ArrayBuffer addAll",
            (start, end) => {
              val positions = Array(
                -0.5f, 0.5f, -0.5f, -0.5f, 0.5f, -0.5f, 0.5f, 0.5f
              )
              val positionsAggregate = new ArrayBuffer[Float]()
              start()
              for (i <- 0 until N) {
                positionsAggregate.addAll(positions)
              }
              end()
            }
          )
      ),
      (
        "PreallocatedArray".toLowerCase(),
        () =>
          test(
            "Preallocated Array",
            (start, end) => {
              val positions = Array(
                -0.5f, 0.5f, -0.5f, -0.5f, 0.5f, -0.5f, 0.5f, 0.5f
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
            }
          )
      )
    )
  )

  private def test(name: String, action: (() => Unit, () => Unit) => Unit) = {
    val times = ArrayBuffer[Double]()
    for (i <- 0 until RUNS) {
      var timeStart = 0L
      var timeElapsed = 0.0
      action(
        () => timeStart = System.nanoTime(),
        () => timeElapsed = (System.nanoTime() - timeStart) / 1e9d
      )
      times.addOne(timeElapsed)
    }
    val averageTime = times.reduce((a, b) => a + b) / times.length
    println(name + ": Avg " + averageTime + "s")
  }
}
