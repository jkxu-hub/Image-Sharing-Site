package Backend.Models

import scala.collection.mutable.ListBuffer

/** A database of sorts, that persists as long as the server process is running. All data is loss once the server terminates.*/
object Database {
  //hashmap id to request buffer
  //TODO you should probably change this to a set
  val imageNames = ListBuffer("cat.jpg","dog.jpg", "eagle.jpg", "elephant.jpg", "flamingo.jpg", "kitten.jpg", "parrot.jpg", "rabbit.jpg")
  val htmlClientNames = new StringBuilder("")
  val htmlClientImages = new StringBuilder("")
  val htmlComments = new StringBuilder("")
  val tokens = new ListBuffer[String]()

  // Save to database


}
