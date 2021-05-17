package client

import java.io.{BufferedReader, IOException, InputStreamReader, PrintWriter}
import java.net.Socket

object Network extends Thread {
  override def run() = {
    try {
      val socket = new Socket("localhost", 4444)
      val out = new PrintWriter(socket.getOutputStream(), true)
      val in = new BufferedReader(
        new InputStreamReader(socket.getInputStream())
      )

      var inputLine = in.readLine()
      while (inputLine != null) {
        System.out.println("Server Says: " + inputLine)
        inputLine = in.readLine()
      }
      socket.close()
    } catch {
      case e: IOException => println(e)
    }
  }
}
