package Backend.Models
import java.sql.{Connection, DriverManager, ResultSet, SQLException}
import scala.collection.mutable.ArrayBuffer
import Backend.Models.{json_creation => json_create}

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
    createUsersTable()
    createAuthenticatedUsersTable()
  }

  /*----------------------- authenticated users table below ----------------------------*/
  /** Creates the authUsers table. A table containing the primary id used to identify users, as well as
   * username, password, authentication cookie, etc.
   * */
  private def createAuthenticatedUsersTable(): Unit = {
    connection.createStatement().execute("CREATE TABLE IF NOT EXISTS authUsers(user_id INT NOT NULL AUTO_INCREMENT, username VARCHAR(20), email TEXT, password_hash TEXT, password_salt TEXT, auth_token TEXT, token_salt TEXT, PRIMARY KEY (user_id), UNIQUE(username))")
  }

  /**
   * Inserts a newly created user into the authUsers table
   * */
  def insertAuthenticatedUser(username: String, email: String, pass_hash: String, pass_salt: String, auth_token: String, token_salt: String): Boolean = {
    try {
      val statement = connection.prepareStatement("INSERT INTO authUsers (username, email, password_hash, password_salt, auth_token, token_salt) VALUES (?, ?, ?, ?, ?, ?)")
      statement.setString(1, username)
      statement.setString(2, email)
      statement.setString(3, pass_hash)
      statement.setString(4, pass_salt)
      statement.setString(5, auth_token)
      statement.setString(6, token_salt)
      statement.execute()
      true
    }catch{
      case s: SQLException =>
        println(s)
        false
    }

  }
  /** Lists the hashed password and salt associate with the username.
   * @param username
   * @return (hashed_password, salt) if the password and hash are found. null otherwise.
   * */
  def listAuthenticatedPasswordHash(username: String): String = {
    val statement = connection.prepareStatement("Select password_hash FROM authUsers where username = ?")
    statement.setString(1, username)
    val res: ResultSet = statement.executeQuery()
    try {
      res.next()
      val password_hash = res.getString("password_hash")

      password_hash
    } catch {
      case s: SQLException =>
        println(s)
        null
    }
  }

  /** Lists the id that will be used in session cookies */
  def listAuthenticatedId(username: String): Unit = {

  }

  /** Lists the username that will be displayed on the index page */
  def listAuthenticatedUsername(id: Int): Unit = {

  }

  /** Updates the token and salt of the authenticated user */
  def updateAuthenticatedUserToken(id: Int, auth_token: String, token_salt: String): Unit = {

  }


  /*---------------------- users table SQL below--------------------------*/
  /** Creates users table. users(user_id, username, email) Called by init_database().*/
  private def createUsersTable(): Unit = {
    connection.createStatement().execute("CREATE TABLE IF NOT EXISTS users (user_id INT NOT NULL AUTO_INCREMENT, username VARCHAR(255), email TEXT, PRIMARY KEY (user_id), UNIQUE(username))")
  }

  /** Insert new user. Called when POST /users request is received.
   * @return the JSON of the newly created record
   * */
  def insertNewUser(username: String, email: String): String = {
    val statement = connection.prepareStatement("INSERT INTO users (username, email) VALUE (?, ?)")
    statement.setString(1, username)
    statement.setString(2, email)
    statement.execute()
    val statement2 = connection.prepareStatement("SELECT * FROM users WHERE username = ?")
    statement2.setString(1, username)
    val user: ResultSet = statement2.executeQuery()
    val json_str = json_create.user_entry_string(user)
    json_str
  }

  /** List all created users. Called when GET /users request is received by server.
   *
   * @return The JSON string representing all users in the table.
   *         e.g. [{“id”:1, “email”: “firstuser@example.com”, “username”: “coolguy123”}, {“id”:2,
   *                  “email”: “cse312@example.com”, “username”: “cse312”}]
   * */
  def listAllUsers(): String = {
    val statement = connection.createStatement()
    val result: ResultSet = statement.executeQuery("SELECT * FROM users")

    val arr = ArrayBuffer[ujson.Obj]()
    while (result.next()) {
      val user_id = result.getString("user_id")
      val username = result.getString("username")
      val email = result.getString("email")
      arr += ujson.Obj("user_id" -> user_id, "username"-> username, "email" -> email)
    }
    val json_str = ujson.write(arr)
    json_str
  }

  /** List single user. Called when GET /users/id is request is received by the server.
   *
   * @return The JSON string representing an entry matching the id in the users table.
   *         e.g. {“id”:2, “email”: “cse312@example.com”, “username”: “cse312”}
   *
   * */
  def listSingleUser(id: Int): String = {
    val statement = connection.prepareStatement("SELECT * FROM users WHERE user_id = ?")
    statement.setInt(1, id)
    val user: ResultSet = statement.executeQuery()
    try{
      user.next()
      val user_id = user.getString("user_id")
      val username = user.getString("username")
      val email = user.getString("email")
      val json_str = ujson.write(ujson.Obj("user_id" -> user_id, "username" -> username, "email" -> email))
      json_str
    }catch{
      case s: SQLException => println(s); ""
    }

  }

  /** Update user entry. Called when PUT /user/id request is received by the server.
   *
   * @return the JSON of the newly updated user entry.
   * */
  def updateUserEntry(id: Int, username: String, email: String): String = {
    try{
      val statement = connection.prepareStatement("UPDATE users SET username = ?, email = ? where user_id = ?")
      statement.setString(1, username)
      statement.setString(2, email)
      statement.setInt(3, id)
      statement.execute()
      //returns the json of the updated user
      listSingleUser(id)
    }catch{
      case s: SQLException => println(s); ""
    }
  }

  /** Delete user. Called when DELETE /users/id is received by the server.
   *
   * @param id the id of the user we want to delete
   * @return true if the user was successfully deleted otherwise, false
   * */
  def deleteUser(id: Int): Boolean = {
    val statement = connection.prepareStatement("DELETE FROM users WHERE user_id = ?")
    statement.setInt(1, id)
    //returns the number of rows that were affected
    if (statement.executeUpdate() == 0){
      false
    }else{
      true
    }
  }



  /*---------------------- globalMessages table SQL below--------------------------*/
  /** creates the globalMessages table. globalMessages(user_id, username, messages).
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
