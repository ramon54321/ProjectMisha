package client.network

import java.io.{BufferedReader, IOException, InputStreamReader, PrintWriter}
import java.net.Socket
import scala.collection.mutable.Queue

object Network extends Thread {
  private var socket: Socket = null
  private var out: PrintWriter = null
  private var in: BufferedReader = null

  private val messageQueue = new Queue[String]()

  override def run() = {
    try {
      socket = new Socket("localhost", 4444)
      out = new PrintWriter(socket.getOutputStream(), true)
      in = new BufferedReader(
        new InputStreamReader(socket.getInputStream())
      )

      var message = in.readLine()
      while (message != null) {
        messageQueue.enqueue(message)
        message = in.readLine()
      }
      socket.close()
    } catch {
      case e: IOException => println(e)
    }
  }

  def send(message: String): Boolean = {
    if (out == null) return false
    out.println(message)
    return true
  }
}
