package Backend.Controllers
import akka.io.{IO, Tcp}
import akka.actor.{Actor, ActorRef, Props}
import akka.util.ByteString

import java.net.InetSocketAddress
import scala.collection.mutable.Set

/** Handles TCP connections. Assigns a new WebServer Actor for every client process connection. */
class TcpHandler extends Actor {

  import Tcp._
  import context.system

  private val webSocketConnections: Set[ActorRef] = Set[ActorRef]() //TODO change name to websocket connections

  // The TCP manager (which is an actor) handles all low level I/O resources.
  // The TCP manager issues the syscalls to the OS which are responsible for sending and receiving datagrams (packets) and
  // establishing the status of connections
  private val manager = IO(Tcp)
  //Sends a message to the TCP manager to bind the Actor to the address:port, which will now listen for incoming TCP connections
  //at the address:port
  //TODO change this to "0.0.0.0" for docker and port 8000
  manager ! Bind(self, new InetSocketAddress("0.0.0.0", 8000))

  def receive ={
    case b: Bound => println("Listening on Port " + b.localAddress.getPort)
    case c: Connected =>
      // Assigns a webserver actor to act as the webserver for the client
      val webserver = context.actorOf(Props[WebServer])
      sender() ! Register(webserver)
  }

}



/*
object ImageShareAppMain {
  def main(args: Array[String]): Unit = {
    val serverSystem = ActorSystem("ServerSystem")
    serverSystem.actorOf(Props(classOf[TcpHandler]))
    //database.init_database
  }

}

 */

