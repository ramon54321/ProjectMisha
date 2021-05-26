package server

import java.io.{BufferedReader, IOException, InputStreamReader, PrintWriter}
import java.net.Socket

class Client(private val socket: Socket) extends Thread {
  private var out: PrintWriter = null
  private var in: BufferedReader = null

  def send(message: String): Boolean = {
    if (out == null) return false
    out.println(message)
    return true
  }

  override def run() = {
    try {
      out = new PrintWriter(socket.getOutputStream(), true)
      in = new BufferedReader(
        new InputStreamReader(socket.getInputStream())
      )

      var message = in.readLine()
      while (message != null) {
        Server.enqueueClientMessage(this, message)
        message = in.readLine()
      }
    } catch {
      case e: IOException => println(e)
    }
  }
}
