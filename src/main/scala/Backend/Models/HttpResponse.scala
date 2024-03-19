package Backend.Models

import akka.util.ByteString

import java.nio.file.{Files, Paths}
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
/** Contains methods for creating http responses*/
object HttpResponse {
  def buildOKResponseString(mimeType: String, content: String): ByteString = {
    ByteString(buildResponse("200 OK", mimeType, content.length) + content)
  }

  def buildOKResponseBytes(mimeType: String, content: ByteString): ByteString = {
    ByteString(buildResponse("200 OK", mimeType, content.length)) ++ content
  }

  def buildRedirectResponse(redirectLocation: String): ByteString = {
    var response = ""
    response += "HTTP/1.1 301 Moved Permanently\r\n"
    response += "X-Content-Type-Options: nosniff\r\n"
    response += "Content-Length: 0\r\n"
    response += "Location: " + redirectLocation + "\r\n"
    response += "\r\n"
    response
    ByteString(response)
  }

  //TODO split this into two functions: 1. reading image bytes 2. reading text file bytes
  def readFile(fileName: String, isImageFile: Boolean): ByteString = {
    var content = ByteString()
    if (isImageFile) {
      val imageBytes = Files.readAllBytes(Paths.get(fileName))
      content = ByteString(imageBytes)
    } else {
      val bufferedSource = scala.io.Source.fromFile(fileName)
      content = ByteString(bufferedSource.mkString)
      bufferedSource.close()
    }
    content
  }

  def buildNotFoundResponse(mimeType: String, content: String): ByteString = {
    ByteString(buildResponse("404 Not Found", mimeType, content.length) + content)
  }

  def buildForbiddenResponse(mimeType: String, content: String) = {
    ByteString(buildResponse("403 Forbidden", mimeType, content.length) + content)
  }

  def buildBadRequestResponse(mimeType: String, content: String) = {
    ByteString(buildResponse("400 Bad Request", mimeType, content.length) + content)
  }

  def buildResponse(responseCode: String, mimeType: String, contentLen: Int): String = {
    var response = ""
    response += "HTTP/1.1 " + responseCode + "\r\n"
    response += "Content-Type: " + mimeType + "\r\n"
    response += "X-Content-Type-Options: nosniff\r\n"
    response += "Content-Length: " + contentLen.toString + "\r\n"
    response += "\r\n" //these are the two \r\n that separate header from body
    response
  }

  def buildWebsocketUpgradeResponse(accept_key: String): ByteString = {
    var response = ""
    response += "HTTP/1.1 " + "101" + "\r\n"
    response += "Connection: Upgrade\r\n"
    response += "Upgrade: websocket\r\n"
    response += "Sec-WebSocket-Accept: " + accept_key + "\r\n"
    response += "\r\n"
    ByteString(response)
  }

  def buildNoContentResponse(): ByteString = {
    var response = ""
    response += "HTTP/1.1 " + "204" + "\r\n"
    response += "\r\n"
    ByteString(response)

  }

  def buildOKResponseBytesNCookies(mimeType: String, content: ByteString, cookies: ListBuffer[String]): ByteString = {
    var response = ""
    response += "HTTP/1.1 " + "200 OK" + "\r\n"
    response += "Content-Type: " + mimeType + "\r\n"
    response += "X-Content-Type-Options: nosniff\r\n"

    for (cookie <- cookies) {
      response += "Set-Cookie: " + cookie + "; Max-Age=36000; HttpOnly" + "\r\n"
    }
    response += "Content-Length: " + content.length.toString + "\r\n"
    response += "\r\n" //these are the two \r\n that separate header from body
    ByteString(response) ++ content
  }
}
