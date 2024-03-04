package Backend.Models

import scala.collection.mutable.{ArrayBuffer, Map}
import Backend.Models.{security=> sec}
import Backend.Models.{HttpRequest => request}
import Backend.Models.{Database => database}

object SaveFormInfo {
  /** Save image upload form data
   * @return true if the xsrf token was found, false otherwise
   * */
  def save_image_upload_form_data(): Boolean = {
    request.extract_bytes_between_boundaries(ArrayBuffer("xsrf", "image", "name"))
    val xsrfToken = request.extract_text("xsrf")
    if (database.tokens.contains(xsrfToken)) {
      //getting the image and name from the form
      val fileName = request.extract_image("image")
      var nameString = request.extract_text("name")

      //preventing html injection
      nameString = sec.htmlInjectionReplace(nameString)
      database.htmlClientNames.append(nameString + "<br>")

      //add the file that you create after the post request into the template
      val imageHTML: String = "<img src=\"clientPhotos/" + fileName + "\"  class=\"my_image\" />"
      database.htmlClientImages.append(imageHTML)

      //updating global variables
      database.imageNames += fileName

      request.reset_fields()
      true
    }else {
      request.reset_fields()
      false
    }
  }

  /** Save comment form data
   * @return true if the xsrf token was found, false otherwise
   * */
  def save_comment_form_data(): Boolean = {
    request.extract_bytes_between_boundaries(ArrayBuffer("xsrf", "name", "comment"))
    val xsrfToken = request.extract_text("xsrf")
    if (database.tokens.contains(xsrfToken)) {
      var nameString = request.extract_text("name")
      nameString = sec.htmlInjectionReplace(nameString)
      var commentString = request.extract_text("comment")
      commentString = sec.htmlInjectionReplace(commentString)

      database.htmlComments.append(nameString+ ": " + commentString + "<br>")

      request.reset_fields()
      true
    } else {
      request.reset_fields()
      false
    }
  }

}
