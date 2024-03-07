package Backend.Models

object Websocket {
  //Value used to complete websocket handshake
  private val GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"

  /**Completes websocket handshake with the client to upgrade the connection protocol from http to
   * websocket.
   *
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

}
