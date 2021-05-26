package server

import java.io.IOException
import java.net.ServerSocket
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Queue

object Server {
  private var listenerThread: Thread = null
  private var serverSocket: ServerSocket = null

  private val clients = new ArrayBuffer[Client]()
  private val clientMessageQueue = new Queue[(Client, String)]()

  this.listenerThread = new Thread() {
    override def run() = {
      try {
        serverSocket = new ServerSocket(4444)
        while (true) {
          waitForClient()
        }
      } catch {
        case e: IOException => println(e)
      }
    }
  }
  this.listenerThread.start()

  private def waitForClient() = {
    val socket = serverSocket.accept()
    val client = new Client(socket)
    client.start()
    clients.addOne(client)
  }

  def enqueueClientMessage(client: Client, message: String) = {
    clientMessageQueue.enqueue((client, message))
  }

  def broadcast(message: String) = {
    clients.foreach(client => client.send(message))
  }

  def shutdown() = {
    try {
      listenerThread.interrupt()
      clients.foreach(client => client.interrupt())
      serverSocket.close()
    } catch {
      case e: IOException => println(e)
    }
  }
}
