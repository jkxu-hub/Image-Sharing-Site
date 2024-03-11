package Backend.Views

import scala.collection.mutable.ArrayBuffer

/** Generates the html that will be used with our html templating engine
 *
 * */
object htmlGenerator {

  def generate_global_chats_html(allUsersAndMessages: ArrayBuffer[(String,String)]): String = {
    val html = new StringBuilder("")
    for (userMessage <- allUsersAndMessages) {
      html.append(userMessage._1 + ": " + userMessage._2 + "<br>")
    }
    html.toString()
  }

}
