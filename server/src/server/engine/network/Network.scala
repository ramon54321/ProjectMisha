package server.engine.network

import java.io.IOException
import java.net.ServerSocket
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Queue

object Network extends Thread {
  private var listenerThread: Thread = null
  private var serverSocket: ServerSocket = null

  private val clients = new ArrayBuffer[Client]()
  private val clientMessageQueue = new Queue[(Client, String)]()

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
