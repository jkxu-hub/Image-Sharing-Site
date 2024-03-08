package Backend.Models

import akka.util.ByteString

import java.nio.charset.StandardCharsets
import scala.collection.mutable.ArrayBuffer

/** Handles websocket handshake acceptance key generation, websocket frame parsing*/
object Websocket {
  //Value used to complete websocket handshake
  private val GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"
  val text_opcode: Int = 1 //0001
  val close_connection_opcode: Int = 8 //1000

  /**Completes websocket handshake with the client to upgrade the connection protocol from http to
   * websocket.
   *
   * @param request a request object, which contains header information about upgrading websocket connection
   * @return the acceptance key used in the websocket handshake
   * */
  def get_websocket_accept_key(request: Request): String = {
    var accept_str = ""
    //Get Sec-WebSocket-Key header
    //TODO Error handling if "Sec-WebSocket-Key" is not found
    accept_str = request.header_map("Sec-WebSocket-Key")
    //Append GUID to the header key
    accept_str += GUID
    //Computes the SHA -1 hash of this and base 64 encoding
    val md = java.security.MessageDigest.getInstance("SHA-1")
    val processedKey = java.util.Base64.getEncoder.encodeToString(md.digest(accept_str.getBytes()))
    processedKey
  }

  def get_opcode(data: ByteString): Int = {
    val byteArray: Array[Byte] = data.toArray
    val opcode = byteArray(0) & 15
    opcode
  }
  /** Extracts the payload from the websocket frame sent over the TCP socket.
   *  Assumption: the payload will only be text and only sent in a single websocket frame.
   *  TODO: Added parsing for multiple contiguous frames and for images sent over websocket
   * */
  def extract_payload_text(data: ByteString): Array[Byte] = {
    val byteArray: Array[Byte] = data.toArray
    val fin_bit = (byteArray(0) & 128) >> 7
    assert(fin_bit == 1)
    val opcode = byteArray(0) & 15
    println(opcode)
    assert(opcode == 1)
    val mask = ((byteArray(1) & 128) >> 7)
    assert(mask == 1)
    val payload_len = byteArray(1) & 127

    var idx_of_masking_key = 2 //to 6 (non inclusive)
    var idx_of_payload = 6 // to end
    if(payload_len == 126){
      //next 2 bytes is payload
      idx_of_masking_key += 2
      idx_of_payload += 2
    }
    if(payload_len == 127){
      //next 8 bytes is payload
      idx_of_masking_key += 8
      idx_of_payload += 8
    }
    val masking_key = byteArray.slice(idx_of_masking_key, idx_of_payload)
    val masked_payload = byteArray.slice(idx_of_payload, byteArray.length)
    val payload = unmask_payload(masking_key, masked_payload)
    payload
    //new String(payload, StandardCharsets.UTF_8)
  }

  /** Performs a bitwise XOR with the masking_key on the masked_payload*/
  private def unmask_payload(masking_key: Array[Byte], masked_payload: Array[Byte]): Array[Byte] = {
    val ret_payload = ArrayBuffer[Byte]()
    var i = 0
    for (byte <- masked_payload){
      ret_payload += (byte ^ masking_key(i % 4)).toByte
      i += 1
    }
    ret_payload.toArray
  }




  /** Used for debugging. Takes in a byteArray and prints a binary string
   * representing the byte array into stdout.
   * */
  def printToBinaryString(byteArray: Array[Byte]): Unit = {
    var binaryString = ""
    var numZerosPrepend = 0
    var doNewLine = false
    var bytesProcessed = 0
    for (byte <- byteArray) {
      if (byte.toBinaryString.length < 8) {
        numZerosPrepend = 8 - byte.toBinaryString.length
      }
      if (doNewLine) {
        binaryString += ("0" * numZerosPrepend) + byte.toBinaryString + "\n"
        doNewLine = false
      } else {
        binaryString += ("0" * numZerosPrepend) + byte.toBinaryString + "_"
      }
      bytesProcessed += 1
      if (bytesProcessed == 3) {
        bytesProcessed = -1
        doNewLine = true
      }
      numZerosPrepend = 0
    }
    println(byteArray.mkString(" "))
    println(binaryString)
  }

}
