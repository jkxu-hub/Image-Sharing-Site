package Backend.Models

import akka.util.ByteString

import java.io.{ByteArrayInputStream, File}
import java.nio.charset.StandardCharsets
import javax.imageio.ImageIO
import scala.collection.mutable.{ArrayBuffer, Map}
import scala.util.Random
import Backend.Views.{PageDirectories => dirs}

/**This class deals with the request line, headers, and payload of all HTTP requests.
 * It initializes the payload and facilitates buffering. Methods for parsing the specific
 * payload are handled in the Payload object.
 * */
class Request(data: ByteString) {
  var (requestLine, headers, payload): (String, String, Array[Byte])  = ("", "", Array())
  var (method, path, version) = ("","","")
  var header_map: Map[String, String] = Map[String, String]()

  //Initializing
  if (Payload.isBuffering){
    Payload.append_to_buffer(data)
  }else{
    val (r, h, p) = parse_request_bytes(data)
    requestLine = r; headers = h ; payload = p
    val (m, pa, v) = parse_request_line(requestLine)
    method = m; path = pa; version = v
    val h_map: Map[String, String] = put_headers_in_map(headers)
    header_map = h_map
    if (method == "POST"){
      init_payload()
    }
  }

  /** Takes in data sent over the tcp socket and splits it into request line, header, and payload bytes*/
  private def parse_request_bytes(data: ByteString): (String, String, Array[Byte]) = {
    val newline = "\r\n".getBytes()
    val double_newline = "\r\n\r\n".getBytes()
    val data_array = data.to[Array]
    //getting the idx in the data_array where the headers and body start
    val idx_of_headers = data_array.indexOfSlice(newline) + newline.length
    val idx_of_payload = data_array.indexOfSlice(double_newline) + double_newline.length
    val payload_exists = idx_of_payload != -1
    //slicing the array into request line, header, and body (if a body exists)
    val requestLineBytes = data_array.slice(0, idx_of_headers - newline.length)
    if (payload_exists){
      val headerBytes = data_array.slice(idx_of_headers, idx_of_payload - double_newline.length)
      //TODO do we subtract from newline bytes here?
      val bodyBytes = data_array.slice(idx_of_payload, data_array.length)
      (new String(requestLineBytes), new String(headerBytes), bodyBytes)
    }else{
      val headerBytes = data_array.slice(idx_of_headers, idx_of_payload)
      (new String(requestLineBytes), new String(headerBytes), Array())
    }
  }

  /** Takes the bytes of the request line and splits it into request method, path, and version*/
  private def parse_request_line(request_line: String): (String, String, String) = {
    println("request line: " + request_line)
    val request_line_array = request_line.split(' ')
    if (request_line_array.length != 3){
      return ("","","")
    }
    (request_line_array(0), request_line_array(1), request_line_array(2))
  }

  /** Puts the headers into an easy to use map data structure */
  private def put_headers_in_map(headers: String): Map[String, String] = {
    val headerLines: Array[String] = headers.split("\r\n")
    val header_map = Map[String, String]()
    var arraySplit = Array("")
    for (line <- headerLines) {
      arraySplit = line.split(": ")
      if (arraySplit.length > 1) {
        header_map += (arraySplit(0) -> arraySplit(1))
      }
    }
    header_map
  }

  /** Sets the initial values of the post request payload using the initial HTTP Request*/
  private def init_payload(): Unit = {
    Payload.buffer = Payload.buffer ++ ByteString(payload)
    Payload.bytesLeftToRead = header_map("Content-Length").toInt - payload.length
    Payload.boundary = "--" + header_map("Content-Type").split("=")(1)
    Payload.isBuffering = (Payload.bytesLeftToRead > 0)
    Payload.path = path
    Payload.method = method
  }

}

/**An object encompassing the payload of an HTTP request. Allows for buffering to take place for
 * large file uploads.
 * */
object Payload{
  var bytesLeftToRead: Int = 0 //this will be set to the content length aka the # of bytes
  var isBuffering = false
  var buffer: ByteString = ByteString()
  var boundary: String  = ""
  var fieldNameToBytes: Map[String, ArrayBuffer[Byte]] = Map[String, ArrayBuffer[Byte]]()
  var path: String = ""
  var method: String = ""

  /** Appends the bytes of the request to the request buffer and updates
   * bytesLeftToRead.
   *
   * Called when isBuffering is true.
   *
   * @param request_bytes the bytes to be appended
   * */
  def append_to_buffer(request_bytes: ByteString): Unit = {
    //appends addition data to our requestBuffer
    buffer = buffer ++ request_bytes
    //TODO might not be necessary to convert this into an array
    bytesLeftToRead -= request_bytes.toArray.length
    if (bytesLeftToRead <= 0) {
      isBuffering = false
      //formingBuffer = false
    }
  }

  /** Reads the entirety of the buffer which contains the entirety of the Post Request's bytes.
   * Splits the bytes of this buffer into a map mapping the form's fieldNames to the bytes associated with
   * that field name. The split occurs at each WebKitFormBoundary.
   *
   * @param fieldNames The list of fields that are used in the post form. Order of elements matter.
   *                   e.g. ("xsrf", "image", "name")
   * */
  def extract_bytes_between_boundaries(fieldNames: ArrayBuffer[String]): Unit = {
    val boundaryBytes = boundary.getBytes
    val boundaryLen = boundaryBytes.length
    val requestArrayBuff = buffer.to[ArrayBuffer]

    var i = 0
    var name_idx = 0
    var byteBuilder: ArrayBuffer[Byte] = ArrayBuffer[Byte]()

    requestArrayBuff.trimStart(boundaryLen)

    //iterating the bytes of the request array.
    //separating the array into sub arrays as demarcated by the boundary.
    for (byte <- requestArrayBuff) {
      if (byte == boundaryBytes(i)) {
        i += 1
      } else {
        // resets the lookup index
        if (byte == boundaryBytes(0)) {
          i = 1
        } else {
          i = 0
        }
      }

      byteBuilder.append(byte)

      //boundary is found
      if (i == boundaryLen) {
        //remove the trailing boundary
        byteBuilder.trimEnd(boundaryLen)
        //store the array buffer
        fieldNameToBytes(fieldNames(name_idx)) = byteBuilder

        //update values
        byteBuilder = new ArrayBuffer[Byte]()
        name_idx += 1
        i = 0

      }
    }
  }

  /**
   * Strips all headers and unnecessary white spaces
   *
   * example input:
   * ------WebKitFormBoundaryGB9FeCCHBsI6rphU
   * Content-Disposition: form-data; name="xsrf_token"
   *
   * 6fump2yad2ixzBsS8ORoafqUiu2i5gWOzra
   *
   * example output:
   * 6fump2yad2ixzBsS8ORoafqUiu2i5gWOzra
   *
   * @param bytes The bytes contained in one of the kv pairs in fieldNameToBytes
   * */
  private def extract_data(bytes: ArrayBuffer[Byte]): ArrayBuffer[Byte] = {
    //val form_text = new String(bytes.toArray, StandardCharsets.UTF_8)
    val singleNewLineByteLen = "\r\n".getBytes().length
    val newLineBytes = "\r\n\r\n".getBytes()
    val start_idx = bytes.indexOfSlice(newLineBytes) + newLineBytes.length
    val end_idx = bytes.length - singleNewLineByteLen
    bytes.slice(start_idx, end_idx)
  }

  /** Extracts the text from the bytes in fieldNameToBytes.
   *
   * @param name the name of the form's field
   * */
  def extract_text(name: String): String = {
    val bytes = fieldNameToBytes(name)
    val text_bytes = extract_data(bytes)
    new String(text_bytes.toArray, StandardCharsets.UTF_8)
  }

  /** Extracts the image from the bytes in fieldNameToBytes. Then creates an image file with a random
   * name using the extracted bytes.
   *
   * @param name the name of the form's field
   * */
  def extract_image(name: String): String = {
    val bytes = fieldNameToBytes(name)
    val image_bytes = extract_data(bytes)
    //write to file
    val imageBuff = ImageIO.read(new ByteArrayInputStream(image_bytes.toArray))
    val fileName = Random.alphanumeric.take(10).mkString + ".jpg" //generates a random file name
    val outfile = new File(dirs.clientPhotos + "/" + fileName)
    ImageIO.write(imageBuff, "jpg", outfile)

    fileName
  }

  def reset_fields(): Unit = {
    bytesLeftToRead = 0 //this will be set to the content length aka the # of bytes
    isBuffering = false
    buffer = ByteString()
    boundary = ""
    fieldNameToBytes = Map[String, ArrayBuffer[Byte]]()
    method = ""
    path = ""
  }



}


