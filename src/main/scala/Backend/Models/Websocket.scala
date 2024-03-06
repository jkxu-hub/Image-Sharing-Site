package Backend.Models

object Websocket {
  /**Completes websocket handshake with the client to upgrade the connection protocol from http to
   * websocket.
   *
   * */
  def handshake(): Unit = {
    //Get Sec-WebSocket-Key header
    //Append GUID to the header key
    //Computes the SHA -1 hash of this and base 64 encoding
    // Server sends an HTTP response with the headers

  }

}
