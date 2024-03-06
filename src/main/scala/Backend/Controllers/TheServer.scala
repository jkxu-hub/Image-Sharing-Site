package Backend.Controllers

//TODO make sure the package is correct
import java.io.{ByteArrayInputStream, File}

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.io.{IO, Tcp}
import akka.util.ByteString
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

import javax.imageio.ImageIO

import scala.collection.mutable.{ArrayBuffer, ListBuffer, Map}
import scala.io.Source
import scala.util.Random
import Backend.Models.{security=> sec}
import Backend.Models.{HttpResponse => response}
import Backend.Models.{HttpRequest => request}
import Backend.Models.{Database => database}
import Backend.Models.{StringProcessing => StrProcessing}
import Backend.Models.{SaveFormInfo => forms}
import Backend.Views.templating
import Backend.Views.{PageDirectories => dirs}
import Backend.Models.{Request}

// source: https://doc.akka.io/docs/akka/current/io-tcp.html
// source: https://www.geeksforgeeks.org/java-program-to-convert-file-to-a-byte-array/
// source: https://www.youtube.com/watch?v=qB3O9gjp1gI&list=PLOLBRzMrfILfSA4w3ObCOK9CSTPkgSXgl&index=8

//2.12.10
//2.13.3
class TheServer extends Actor {

  import Tcp._
  import context.system

  //TODO change this to "0.0.0.0" for docker and port 8000
  IO(Tcp) ! Bind(self, new InetSocketAddress("0.0.0.0", 8001))

  //val imageNames = ListBuffer("cat.jpg","dog.jpg", "eagle.jpg", "elephant.jpg", "flamingo.jpg", "kitten.jpg", "parrot.jpg", "rabbit.jpg") //HW1 Code

  var reqType: String = ""
  var reqPath: String = ""

  //val htmlClientNames = new StringBuilder("")
  //val htmlClientImages = new StringBuilder("")
  //val tokens = new ListBuffer[String]()

  def receive = {
    case b: Bound => println("Listening on Port " + b.localAddress.getPort)

    case PeerClosed => println("Connection Closed: " + sender())

    case c: Connected =>
      println("Client Connected: " + sender() + " (" + c.remoteAddress + ")")
      sender() ! Register(self)

    case r: Received =>
      if(!request.isBuffering){
        println(r.data.utf8String) //debugging line

        //TODO change this to an abstract HTTPRequest class
        val (rType, rPath) = request.get_type_path(r.data)
        reqType = rType
        reqPath = rPath

        if(reqType == "POST"){
          // create new request object
          request.process_initial_post(r.data)
        }
      }else{
        //appends additional data to our requestBuffer
        request.append_to_buffer(r.data)
      }

      //we should do all the parsing here
      //This is what we send at the end
      if(!request.isBuffering && reqType == "POST"){
        reqPath match {
          case "/image-upload"=>
            val success = forms.save_image_upload_form_data()
            if(success){
              //sending a page telling the client that their form has been submitted
              val bufferedSource = scala.io.Source.fromFile(dirs.successfulFormSubmission_page)
              val content = bufferedSource.mkString
              sender() ! Write(response.buildOKResponseBytes("text/html", ByteString(content)))
            }else{
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
          case _ =>
            sender() ! Write(response.buildForbiddenResponse("text/plain", "403 Forbidden. Your submission was REJECTED!"))
        }
      }

      //GET requests are the only reqType for HW1
      if(reqType == "GET"){
        reqPath match {
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
            val content = response.readFile(dirs.images + reqPath.substring("/MultiImageView/image".length), true)
            sender() ! Write(response.buildOKResponseBytes("image/jpeg", content))
          case path if path.length > 13 && path.substring(0, 13) == "/clientPhotos" && database.imageNames.contains(path.substring(14)) =>
            //http://localhost:8002/image/kitten.jpg
            val content = response.readFile(dirs.clientPhotos + reqPath.substring(13), true)
            sender() ! Write(response.buildOKResponseBytes("image/jpeg", content))
          case path if path.length > 7 && path.substring(0, 7) == "/images" =>
            val m = StrProcessing.process_query_string(reqPath)
            val content = templating.populate_imageView_template(m)
            if (content == null){
              sender() ! Write(response.buildNotFoundResponse("text/plain", "404 Content Not Found!"))
            }else{
              sender() ! Write(response.buildOKResponseBytes("text/html", ByteString(content)))
            }
          case "/global-chat" =>
            val bufferedSource = scala.io.Source.fromFile(dirs.globalChatPage)
            val content = bufferedSource.mkString
            sender() ! Write(response.buildOKResponseBytes("text/html", ByteString(content)))
          case "/globalChatFunctions.js" =>
            //TODO is this a security concern. Users can see the entirety of functions.js.
            val content = response.readFile(dirs.globalChatFunctions_js, false)
            sender() ! Write(response.buildOKResponseBytes("text/javascript", content))
          case "/websocket" =>
          //Get Sec-WebSocket-Key header
          //Append GUID to the header key
          //Computes the SHA -1 hash of this and base 64 encoding
          // Server sends an HTTP response with the headers
          case _ =>
            //http://localhost:8002/dsfsdf
            //404 Not Found
            sender() ! Write(response.buildNotFoundResponse("text/plain", "404 Content Not Found!"))
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