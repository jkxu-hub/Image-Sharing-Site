package Backend.Models

import scala.collection.mutable.ArrayBuffer

object WebsocketResponse {


  /** Builds a text response frame. Fin bit will always be 1. The opcode will be 0001. Payload length will be recomputed.
   * @param payload a byte array containing the bytes of the payload we are sending
   * */
  def buildTextResponseFrame(payload: Array[Byte]): Array[Byte] = {
    val response_frame = ArrayBuffer[Byte]()
    // 129 = 1000 0001
    response_frame += 129.toByte
    response_frame += payload.length.toByte
    response_frame ++= payload
    response_frame.toArray
  }

  /** Builds a response frame for closing the websocket connection. Fin bit will always be 1. Opcode will be 1000.
   * No payload or mask.
   * */
  def buildCloseResponseFrame(): Array[Byte] ={
    val response_frame = ArrayBuffer[Byte]()
    // 136 = 1000 1000
    response_frame += 136.toByte
    // mask and payload len is 0
    response_frame += 0.toByte
    response_frame.toArray
  }


}
