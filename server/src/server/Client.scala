package server

import java.io.{BufferedReader, IOException, InputStreamReader, PrintWriter}
import java.net.Socket

class Client(val socket: Socket) extends Thread {
  override def run() = {
    try {
      val out = new PrintWriter(socket.getOutputStream(), true)
      val in = new BufferedReader(
        new InputStreamReader(socket.getInputStream())
      )

      out.println("Hello new client, how are you?")

      var inputLine = in.readLine()
      while (inputLine != null) {
        System.out.println("Client Says: " + inputLine)
        inputLine = in.readLine()
      }
    } catch {
      case e: IOException => println(e)
    }
  }
}
