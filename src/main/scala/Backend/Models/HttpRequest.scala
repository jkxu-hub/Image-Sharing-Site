package Backend.Models

import akka.util.ByteString

import java.io.{ByteArrayInputStream, File}
import java.nio.charset.StandardCharsets
import javax.imageio.ImageIO
import scala.collection.mutable.{ArrayBuffer, Map}
import scala.util.Random
import Backend.Views.{PageDirectories => dirs}

/** Contains methods for parsing http requests
 *  Contains temporary state of Post requests. i.e. will contain a buffer for POST forms that need multiple requests
 *  to transmit all data.
 * */
object HttpRequest {

  var bytesLeftToRead: Int = 0 //this will be set to the content length aka the # of bytes
  var isBuffering = false
  var buffer: ByteString = ByteString()
  var boundary: String  = ""
  var headers: Map[String, String] = Map[String, String]()
  var fieldNameToBytes: Map[String, ArrayBuffer[Byte]] = Map[String, ArrayBuffer[Byte]]()


  /** Splits the input ByteArray into a ByteArray that consists of the bytes before the delimiter and a ByteArray
   * that consists of the Bytes after the delimiter. The delimiter itself is removed.
   * Used primarily to split between form boundaries and newlines.
   *
   * @param byteArray the input byteArray
   * @param delimiter delimits the point where we will split
   * @ return the split arrays
   * */
  def splitArrayRemoveDelimit[T](byteArray: ArrayBuffer[T], delimiter: Array[T]): (ArrayBuffer[T], ArrayBuffer[T]) = {
    val delimiterIdx = byteArray.indexOfSlice(delimiter)
    val splitArray = byteArray.splitAt(delimiterIdx)
    val sizeOfDelimiter = delimiter.length
    splitArray._2.trimStart(sizeOfDelimiter)
    (splitArray._1, splitArray._2)
  }

  /** Stores header values of the post request and stores the remaining bytes in a buffer.
   * Call this method exactly once per post request.
   * Assumes that all of the headers can be gotten without buffering for the actual headers.
   *
   * @param request_bytes the initial bytes of a post request
   * */
  def process_initial_post(request_bytes: ByteString): Unit = {
    val byteReqArray = request_bytes.to[ArrayBuffer]
    val newLineBytes = "\r\n\r\n".getBytes()
    val processedArrays = splitArrayRemoveDelimit(byteReqArray, newLineBytes)
    /*Gets the bytes of the header*/
    val headerBytes = processedArrays._1.toArray
    val header = new String(headerBytes, StandardCharsets.UTF_8)
    /*Gets the bytes after the header*/
    val afterHeader: ArrayBuffer[Byte] = processedArrays._2

    val headerLines: Array[String] = header.split("\r\n")
    //val headers = Map[String, String]()
    var arraySplit = Array("")
    for (line <- headerLines) {
      arraySplit = line.split(": ")
      if (arraySplit.length > 1) {
        headers += (arraySplit(0) -> arraySplit(1))
      }
    }
    val Content_Length = headers("Content-Length")
    val Content_Type = headers("Content-Type")
    //setting the amount of bytes that this Post request or series of post requests will contain
    bytesLeftToRead = Content_Length.toInt
    //TODO might want to get the multipart/form-data in a future HW
    boundary = "--" + Content_Type.split("=")(1)

    //Appending additional bytes to our buffer
    buffer = buffer ++ ByteString(afterHeader.toArray)
    bytesLeftToRead -= afterHeader.length
    if (bytesLeftToRead > 0) {
      isBuffering = true
    }
  }

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
   * e.g. ("xsrf", "image", "name")
   * */
  def extract_bytes_between_boundaries(fieldNames: ArrayBuffer[String]): Unit = {
    val boundaryBytes= boundary.getBytes
    val boundaryLen = boundaryBytes.length
    val requestArrayBuff = buffer.to[ArrayBuffer]

    var i = 0
    var name_idx = 0
    var byteBuilder: ArrayBuffer[Byte] = ArrayBuffer[Byte]()

    requestArrayBuff.trimStart(boundaryLen)

    //iterating the bytes of the request array.
    //separating the array into sub arrays as demarcated by the boundary.
    for(byte <- requestArrayBuff){
      if(byte == boundaryBytes(i)){
        i+= 1
      }else {
        // resets the lookup index
        if (byte == boundaryBytes(0)){
          i = 1
        }else{
          i = 0
        }
      }

      byteBuilder.append(byte)

      //boundary is found
      if(i == boundaryLen){
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
    headers= Map[String, String]()
    fieldNameToBytes= Map[String, ArrayBuffer[Byte]]()
  }

  /** Gets the request type and path sent in the http request.
   *
   * */
  def get_type_path(bytes: ByteString): (String, String) = {
    val firstLineReq = bytes.utf8String.split(" ", 3)
    (firstLineReq(0), firstLineReq(1))
  }




  //def clearObjectData
  //Maybe this object should be a class so the object just restarts after each post request?



}
