package Backend.Views

import scala.collection.mutable.{ArrayBuffer, Map}
import scala.util.Random
import Backend.Models.{Database_Updated => database_u, Database => database}
import Backend.Views.{PageDirectories => dirs, htmlGenerator => htmlGen}

/**Should this go in models since it interacts with the database?*/
object templating {


  /** populates template.html template
   *
   * @param m a mapping of the things to input
   *          e.g. HashMap(images -> ArrayBuffer(cat, kitten), name -> ArrayBuffer(Mitch))
   * @return returns true upon successful completion. returns false if an image not in the database
   *         was attempted to be accessed
   * */
  def populate_imageView_template(m: Map[String, ArrayBuffer[String]]): String ={
    val name = m("name")(0)
    val image_map = m("images")

    val bufferedSource = scala.io.Source.fromFile(dirs.multiImageView_page)
    var content = bufferedSource.mkString
    content = content.replace("{{name}}", name)

    val htmlImageString = new StringBuilder("")
    for (image <- image_map) {
      if(!database.imageNames.contains(image + ".jpg")){
        return null
      }
      htmlImageString.append("<img src=\"image/" + image + ".jpg\"  class=\"my_image\" />")
    }

    content = content.replace("{{images}}", htmlImageString.toString())
    bufferedSource.close()
    content
  }

  /** Populates the index.html page with a randomly generated token to prevent xsrf
   * and populates the page with the htmlClientNames and htmlClientImages.
   *
   * */
  def populate_index_template(visits_cookie: String): String = {
    val bufferedSource = scala.io.Source.fromFile(dirs.home_page)
    var content = bufferedSource.mkString
    content = content.replace("{{name}}", database.htmlClientNames)
    content = content.replace("{{images}}", database.htmlClientImages)
    content = content.replace("{{comments}}", database.htmlComments)
    //adding an XSRF token for cross site validation to each form
    //generates a random string between 26-36 characters
    val randomToken1: String = Random.alphanumeric.take(26 + Random.nextInt(10)).mkString
    val randomToken2: String = Random.alphanumeric.take(26 + Random.nextInt(10)).mkString
    database.tokens += randomToken1
    database.tokens += randomToken2

    content = content.replace("{{token1}}", randomToken1)
    content = content.replace("{{token2}}", randomToken2)
    content = content.replace("{{visits}}", visits_cookie)

    content
  }

  /** Populates the global chat template with all of the messages stored in the globalMessages table
   *
   * @return the updated contents of the global chat page in the form of a string
   * */
  def populate_global_chat_page(): String = {
    val bufferedSource = scala.io.Source.fromFile(dirs.globalChatPage)
    var content = bufferedSource.mkString
    val allUsersAndMessages = database_u.listAllMessages()
    content = content.replace("{{all messages}}", htmlGen.generate_global_chats_html(allUsersAndMessages))
    content
  }




}
