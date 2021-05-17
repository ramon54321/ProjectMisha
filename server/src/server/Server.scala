package server

import java.io.IOException
import java.net.ServerSocket
import java.util.{ArrayList, List}

class Server {

  private var listenerThread: Thread = _
  private var serverSocket: ServerSocket = _

  private val clients: List[Client] = new ArrayList()

  def start() = {
    val self = this
    this.listenerThread = new Thread() {
      override def run() = {
        try {
          self.serverSocket = new ServerSocket(4444)
          while (true) {
            self.waitForClient()
          }
        } catch {
          case e: IOException => println(e)
        }
      }
    }
    this.listenerThread.start()
  }

  private def waitForClient() = {
    val socket = this.serverSocket.accept()
    val client = new Client(socket)
    client.start()
    this.clients.add(client)
  }

  def shutdown() = {
    try {
      this.listenerThread.interrupt()
      this.clients.forEach(client => client.interrupt())
      this.serverSocket.close()
    } catch {
      case e: IOException => println(e)
    }
  }
}
