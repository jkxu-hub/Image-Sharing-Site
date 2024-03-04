package Backend.Models

import scala.collection.mutable.{ArrayBuffer, Map}
import Backend.Models.{security=> sec}

object StringProcessing {

  /** Takes in a query string, parses it, and populates a hash map with its content.
   *
   * @param path the path which contains the query string we will process
   * */
  def process_query_string(path: String): Map[String, ArrayBuffer[String]] = {
    val q_map: Map[String, ArrayBuffer[String]] = Map[String, ArrayBuffer[String]]()
    var str_buffer: StringBuilder = new StringBuilder("")
    var curr_key = ""
    for (c <- path) {
      c match {
        case '?' =>
          str_buffer = new StringBuilder("")
        case '=' =>
          var str = str_buffer.toString()
          str = sec.htmlInjectionReplace(str)
          q_map(str) = new ArrayBuffer[String]()
          curr_key = str
          str_buffer = new StringBuilder("")
        case  x if x == '+' || x == '&' =>
          var str = str_buffer.toString()
          str = sec.htmlInjectionReplace(str)
          q_map(curr_key).append(str)
          str_buffer = new StringBuilder("")
        case _ =>
          str_buffer += c
      }
    }
    // accounts for the last value of a key val pair
    var str = str_buffer.toString()
    str = sec.htmlInjectionReplace(str)
    q_map(curr_key).append(str)

    q_map
  }


  /**
   * a method that parses the query string
   * iterate each character of the string. looks for keywords =, +, & and
   *
   *
   * sanitizes the user inputted data
   *
   *
   *
   *
   * a method that fills in the html template with the values that we extracted
   *
   *
   *
   *
   * */


}
