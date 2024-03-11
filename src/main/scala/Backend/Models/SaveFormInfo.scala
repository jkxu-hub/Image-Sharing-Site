package Backend.Models

import scala.collection.mutable.{ArrayBuffer, Map}
import Backend.Models.{security => sec}

import java.nio.charset.StandardCharsets
//import Backend.Models.{HttpRequest => request}
import Backend.Models.{Database => database}
import Backend.Models.{Payload}

object SaveFormInfo {
  /** Save image upload form data
   * @return true if the xsrf token was found, false otherwise
   * */
  def save_image_upload_form_data(): Boolean = {
    Payload.extract_bytes_between_boundaries(ArrayBuffer("xsrf", "image", "name"))
    val xsrfToken = Payload.extract_text("xsrf")
    println(xsrfToken)
    if (database.tokens.contains(xsrfToken)) {
      //getting the image and name from the form
      val fileName = Payload.extract_image("image")
      var nameString = Payload.extract_text("name")

      //preventing html injection
      nameString = sec.htmlInjectionReplace(nameString)
      database.htmlClientNames.append(nameString + "<br>")

      //add the file that you create after the post request into the template
      val imageHTML: String = "<img src=\"clientPhotos/" + fileName + "\"  class=\"my_image\" />"
      database.htmlClientImages.append(imageHTML)

      //updating global variables
      database.imageNames += fileName

      Payload.reset_fields()
      true
    }else {
      Payload.reset_fields()
      false
    }
  }

  /** Save comment form data
   * @return true if the xsrf token was found, false otherwise
   * */
  def save_comment_form_data(): Boolean = {
    Payload.extract_bytes_between_boundaries(ArrayBuffer("xsrf", "name", "comment"))
    val xsrfToken = Payload.extract_text("xsrf")
    if (database.tokens.contains(xsrfToken)) {
      var nameString = Payload.extract_text("name")
      nameString = sec.htmlInjectionReplace(nameString)
      var commentString = Payload.extract_text("comment")
      commentString = sec.htmlInjectionReplace(commentString)

      database.htmlComments.append(nameString+ ": " + commentString + "<br>")

      Payload.reset_fields()
      true
    } else {
      Payload.reset_fields()
      false
    }
  }

  /** Takes in a byte array which contains the json message sent by the client. And sanitizes
   * it from html and sql injections.
   *
   * @param payload a byte array which contains the json message sent by the client.
   * @return The byte array with the sanitized json data.
   * */
  def save_chat_message_data(payload: Array[Byte], database_u: Database_Updated): Array[Byte] = {
    val payload_str = new String(payload, StandardCharsets.UTF_8)
    // extract json of payload str
    val json: ujson.Value = ujson.read(payload_str)
    // sanitize the extracted json
    val username = sec.htmlInjectionReplace(json("username").str)
    val comment = sec.htmlInjectionReplace(json("comment").str) //TODO rename this to message
    // Insert username and comment into database
    database_u.insertMessage(username, comment)
    // re-serialize the values
    json("username") = username
    json("comment") = comment
    val result_json = ujson.write(json)
    result_json.getBytes() // return byte array of json
  }

}
