package Backend.Models

/** Does not hold any state, just contains helper methods pertaining to security */
object security {


  /**  Sanitizes user input that is meant to be embedded in html the server sends*/
  def htmlInjectionReplace(str: String): String = {
    // TODO could be more efficient if you loop through the string once instead of multiple times
    str.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
  }
}
