package Backend.Controllers

//TODO make sure the package is correct
import java.io.{ByteArrayInputStream, File}
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.io.{IO, Tcp}
import akka.util.ByteString

import java.net.InetSocketAddress
import Backend.Models.{Database_Updated => database_u, Payload, Request, Websocket, WebsocketResponse, Database => database, HttpResponse => response, SaveFormInfo => forms, StringProcessing => StrProcessing, security => sec}
import Backend.Views.templating
import Backend.Views.{PageDirectories => dirs}

import java.nio.charset.StandardCharsets
import scala.collection.mutable.Set
import scala.util.matching.Regex


// source: https://doc.akka.io/docs/akka/current/io-tcp.html
// source: https://www.geeksforgeeks.org/java-program-to-convert-file-to-a-byte-array/
// source: https://www.youtube.com/watch?v=qB3O9gjp1gI&list=PLOLBRzMrfILfSA4w3ObCOK9CSTPkgSXgl&index=8

//2.12.10
//2.13.3
class TheServer extends Actor {

  import Tcp._
  import context.system

  private val webSocketActors: Set[ActorRef] = Set[ActorRef]() //TODO change name to websocket connections

  // The TCP manager (which is an actor) handles all low level I/O resources.
  // The TCP manager issues the syscalls to the OS which are responsible for sending and receiving datagrams (packets) and
  // establishing the status of connections
  private val manager = IO(Tcp)
  //Sends a message to the TCP manager to bind the Actor to the address:port, which will now listen for incoming TCP connections
  //at the address:port
  //TODO change this to "0.0.0.0" for docker and port 8000
   manager ! Bind(self, new InetSocketAddress("0.0.0.0", 8000))

  def receive = {
    case b: Bound => println("Listening on Port " + b.localAddress.getPort)

    case PeerClosed => println("Connection Closed: " + sender())
    //context.stop(self)
    //TODO removeWebSocket Actor?

    case c: Connected =>
      //println("Client Connected: " + sender() + " (" + c.remoteAddress + ")")
      sender() ! Register(self)

    case r: Received =>
      //In this context sender() refers to whoever sent the Received message to TheWebserver Actor.
      //In this case the sender() is the client.
      //print(sender())
      if(!Payload.isBuffering){
        //println(r.data.utf8String)
      }

      if (webSocketActors.contains(sender())) { //signifies websocket connection
        print(" WEBSOCKET ")
        // extract opcode
        val opcode = Websocket.get_opcode(r.data)
        opcode match {
          case Websocket.text_opcode =>
            //TODO limit the size of the message that can be sent over
            // extract payload
            val payload = Websocket.extract_payload_text(r.data)
            val sanitized_payload = forms.save_chat_message_data(payload)
            // build response with the sanitized payload
            val response_frame = WebsocketResponse.buildTextResponseFrame(sanitized_payload)
            // send response to all websocket connections
            webSocketActors.foreach(connection => connection ! Write(ByteString(response_frame)))
          // how does the database work to display the websocket information?
          case Websocket.close_connection_opcode =>
            // send a close response to the sender()
            sender() ! Write(ByteString(WebsocketResponse.buildCloseResponseFrame()))
            // remove the sender() from the list of webSocket actors
            webSocketActors -= sender()
            //connection close frame
            self ! PeerClosed
          case _ => //TODO add additional opcode handling
            // remove the sender() from the list of webSocket actors
            webSocketActors -= sender()
            //connection close frame
            self ! PeerClosed
        }
        //if opcode == 1, then text frame
        //if opcode == 2, then binary frame
        //if opcode == 8, then connection close frame
        //if opcode == 9, ping frame
        //if opcode == 10, pong frame
      }else{
        print(" HTTP ")
        val req = new Request(r.data)
        if (!Payload.isBuffering && Payload.method == "POST") {
          Payload.path match {
            case "/image-upload" =>
              val success = forms.save_image_upload_form_data()
              if (success) {
                //sending a page telling the client that their form has been submitted
                val bufferedSource = scala.io.Source.fromFile(dirs.successfulFormSubmission_page)
                val content = bufferedSource.mkString
                sender() ! Write(response.buildOKResponseBytes("text/html", ByteString(content)))
              } else {
                sender() ! Write(response.buildForbiddenResponse("text/plain", "403 Forbidden. Your submission was REJECTED!"))
              }
            case "/comment" =>
              val success = forms.save_comment_form_data()
              if (success) {
                val bufferedSource = scala.io.Source.fromFile(dirs.successfulFormSubmission_page)
                val content = bufferedSource.mkString
                sender() ! Write(response.buildOKResponseBytes("text/html", ByteString(content)))
              } else {
                sender() ! Write(response.buildForbiddenResponse("text/plain", "403 Forbidden. Your submission was REJECTED!"))
              }
            case "/users" =>
              // extract payload
              val payload_str = Payload.buffer.utf8String
              val json: ujson.Value = ujson.read(payload_str)
              // sanitize the extracted json
              val email = sec.htmlInjectionReplace(json("email").str)
              val username = sec.htmlInjectionReplace(json("username").str)
              val json_ret = database_u.insertNewUser(username, email)
              sender() ! Write(response.buildOKResponseString("text/plain", json_ret))

            case _ =>
              sender() ! Write(response.buildForbiddenResponse("text/plain", "403 Forbidden. Your submission was REJECTED!"))
          }
        } else if (req.method == "GET") {
          req.path match {
            case "/" =>
              val content = templating.populate_index_template()
              sender() ! Write(response.buildOKResponseBytes("text/html", ByteString(content)))
            case "/hello" =>
              sender() ! Write(response.buildOKResponseString("text/plain", "ni hao world"))
            case "/hi" =>
              sender() ! Write(response.buildRedirectResponse("/hello"))
            case "/functions.js" =>
              //TODO is this a security concern. Users can see the entirety of functions.js.
              val content = response.readFile(dirs.functions_js, false)
              sender() ! Write(response.buildOKResponseBytes("text/javascript", content))
            case "/style.css" =>
              val content = response.readFile(dirs.style_css, false)
              sender() ! Write(response.buildOKResponseBytes("text/css", content))
            case "/utf.txt" =>
              val content = response.readFile(dirs.utf_txt, false)
              sender() ! Write(response.buildOKResponseBytes("text/plain; charset=utf-8", content))
            case path if path.length > "/MultiImageView/image".length && path.substring(0, "/MultiImageView/image".length) == "/MultiImageView/image" && database.imageNames.contains(path.substring("/MultiImageView/image".length + 1)) =>
              //http://localhost:8002/image/kitten.jpg
              val content = response.readFile(dirs.images + req.path.substring("/MultiImageView/image".length), true)
              sender() ! Write(response.buildOKResponseBytes("image/jpeg", content))
            case path if path.length > 13 && path.substring(0, 13) == "/clientPhotos" && database.imageNames.contains(path.substring(14)) =>
              //http://localhost:8002/image/kitten.jpg
              val content = response.readFile(dirs.clientPhotos + req.path.substring(13), true)
              sender() ! Write(response.buildOKResponseBytes("image/jpeg", content))
            case path if path.length > 7 && path.substring(0, 7) == "/images" =>
              val m = StrProcessing.process_query_string(req.path)
              val content = templating.populate_imageView_template(m)
              if (content == null) {
                sender() ! Write(response.buildNotFoundResponse("text/plain", "404 Content Not Found!"))
              } else {
                sender() ! Write(response.buildOKResponseBytes("text/html", ByteString(content)))
              }
            case "/global-chat" =>
              val content = templating.populate_global_chat_page()
              sender() ! Write(response.buildOKResponseBytes("text/html", ByteString(content)))
            case "/globalChatFunctions.js" =>
              //TODO is this a security concern. Users can see the entirety of functions.js.
              val content = response.readFile(dirs.globalChatFunctions_js, false)
              sender() ! Write(response.buildOKResponseBytes("text/javascript", content))
            case "/websocket" =>
              if (!webSocketActors.contains(sender())) {
                val accept_key = Websocket.get_websocket_accept_key(req)
                sender() ! Write(response.buildWebsocketUpgradeResponse(accept_key))
                webSocketActors += sender()
              }
            case "/users" =>
              val json_ret = database_u.listAllUsers()
              sender() ! Write(response.buildOKResponseString("text/plain", json_ret))
            case path if path.matches("\\/users\\/\\d+") =>
              val id = path.substring("/users/".length).toInt
              val json_ret = database_u.listSingleUser(id)
              if(json_ret == ""){
                sender() ! Write(response.buildNotFoundResponse("text/plain", "404 Content Not Found!"))
              }else{
                sender() ! Write(response.buildOKResponseString("text/plain", json_ret))
              }
            case _ =>
              //404 Not Found
              sender() ! Write(response.buildNotFoundResponse("text/plain", "404 Content Not Found!"))
          }
        } else if (!Payload.isBuffering && Payload.method == "PUT"){
          Payload.path match {
            case path if path.matches("\\/users\\/\\d+") =>
              val id = path.substring("/users/".length).toInt
              val payload_str = Payload.buffer.utf8String
              println(payload_str)
              val json: ujson.Value = ujson.read(payload_str)
              // sanitize the extracted json
              val email = sec.htmlInjectionReplace(json("email").str)
              val username = sec.htmlInjectionReplace(json("username").str)

              val json_ret = database_u.updateUserEntry(id, username, email)
              if (json_ret == "") {
                sender() ! Write(response.buildNotFoundResponse("text/plain", "404 Content Not Found!"))
              } else {
                sender() ! Write(response.buildOKResponseString("text/plain", json_ret))
              }
              Payload.reset_fields()
            case _ =>
              sender() ! Write(response.buildNotFoundResponse("text/plain", "404 Content Not Found!"))
          }

        } else if(req.method == "DELETE"){
          req.path match {
            case path if path.matches("\\/users\\/\\d+") =>
              val id = path.substring("/users/".length).toInt
              val success = database_u.deleteUser(id)
              if (success){
                sender() ! Write(response.buildNoContentResponse())
              }else{
                sender() ! Write(response.buildNotFoundResponse("text/plain", "404 Content Not Found!"))
              }
            case _ =>
              sender() ! Write(response.buildNotFoundResponse("text/plain", "404 Content Not Found!"))

          }

        }

      }
  }
}

object TheServer {
  def main(args: Array[String]): Unit = {
    val actorSystem = ActorSystem()
    actorSystem.actorOf(Props(classOf[TheServer]))

  }
}