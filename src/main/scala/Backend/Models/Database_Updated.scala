package Backend.Models
import java.sql.{Connection, DriverManager, ResultSet}
import scala.collection.mutable.ArrayBuffer

object Database_Updated {
  //TODO: change to these values for docker (instead of "localhost" change it to the name of the service)

  //private val db_url = "jdbc:mysql://mysql:3306/todo" //TODO Change for DOCKER
  private val db_url = "jdbc:mysql://localhost:3306/imagesharingbase"
  private val db_username: String = sys.env("DEV_DB_USERNAME")
  private val db_password: String = sys.env("DEV_DB_PASSWORD")
  private val connection: Connection = DriverManager.getConnection(db_url, db_username, db_password)

  init_database()

  /**Initializes the database with all of the tables needed. Called on first instance of database. */
  private def init_database(): Unit = {
    //TODO do you need to create the database here?
    createGlobalMessagesTable()
  }

  /**init_database
   * Creates all tables needed by the database if those tables have not been created yet
   * */
  private def createGlobalMessagesTable(): Unit = {
    connection.createStatement().execute("CREATE TABLE IF NOT EXISTS globalMessages (username TEXT, message TEXT)")
  }

  /**Inserts the username and message into globalMessages table. Called whenever a global message is sent to the server*/
  def insertMessage(username: String, message: String): Unit = {
    val statement = connection.prepareStatement("INSERT INTO globalMessages VALUE (?, ?)")
    statement.setString(1, username)
    statement.setString(2, message)
    statement.execute()
  }

  /**Lists all messages in the globalMessages table. Called on load of the global chat page.
   * The messages will be displayed using templating class.
   * @return an array buffer containing all username and messages in globalMessages
   * */
  def listAllMessages(): ArrayBuffer[(String,String)] = {
    val statement = connection.createStatement()
    val result: ResultSet = statement.executeQuery("SELECT * FROM globalMessages")
    val allUsersAndMessages: ArrayBuffer[(String,String)] = ArrayBuffer[(String,String)]()
    while (result.next()) {
      val username = result.getString("username")
      val comment = result.getString("message")
      allUsersAndMessages.append((username, comment))
    }
    allUsersAndMessages
  }

}
