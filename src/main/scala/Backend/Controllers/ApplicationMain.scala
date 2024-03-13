package Backend.Controllers
import akka.actor.{ActorSystem, Props}

object ApplicationMain {
  def main(args: Array[String]): Unit = {
    val serverSystem = ActorSystem("ServerSystem")
    serverSystem.actorOf(Props(classOf[TcpHandler]))
    //database.init_database
  }
}
