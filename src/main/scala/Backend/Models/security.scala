package Backend.Models
import akka.util.ByteString
import org.mindrot.jbcrypt.BCrypt

import scala.util.Random
import Backend.Models.{Database_Updated => database_u}
import org.joda.time.DateTime

import java.sql.Timestamp

/** Does not hold any state, just contains helper methods pertaining to security */
object security {


  /**  Sanitizes user input that is meant to be embedded in html the server sends*/
  def htmlInjectionReplace(str: String): String = {
    // TODO could be more efficient if you loop through the string once instead of multiple times
    str.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
  }

  /** Generates a hash and salt for a given password or authentication token.
   *
   * @return A tuple containing the hashed password and salt (hash, salt).
   * */
  def generateHashAndSalt(password: String): (String,String)= {
    val salt = BCrypt.gensalt()
    val hash = BCrypt.hashpw(password, salt)
    (hash, salt)
  }

  /** 26^40 possible combinations. Used as an auth token.*/
  def generateRandomToken(): String = {
    Random.alphanumeric.take(41 + Random.nextInt(10)).mkString
  }

  /** Takes in the payload which contains the JSON object with the login information and returns
   * a status code based on the the JSON object.
   *
   * @param payload The payload that contains the JSON object with the login information
   * @return a message with the status. "OK" means that the passwords match. Anything else is an error.
   *
   * */
  def checkPasswordMatch(username: String, password: String): String = {
    val password_hash = database_u.listAuthenticatedPasswordHash(username)
    if(password_hash == null){
      return "No account associated with: " + username
    }

    val passwordCheckRes = BCrypt.checkpw(password, password_hash)
    if(passwordCheckRes == true){
      "OK"
    }else{
      "Incorrect Password. Try Again."
    }
  }

  /** Checks that a token entry exists at the user_id and token_id. And if it exists that
   * the auth_token is not expired and matches the token hash in the database.
   *
   * */
  def isValidToken(user_id: Int, token_id: String, auth_token:String): Boolean = {
    val token_id_int = token_id.toInt
    val token_hash = database_u.listTokenHash(user_id, token_id_int)
    val token_exp: Timestamp = database_u.listTokenExpiration(user_id, token_id_int)

    if(token_hash == null || token_exp == null){
      // returns false if no token found
      return false
    }
    val curr_time = DateTime.now()
    val expired = curr_time.isAfter(token_exp.getTime)

    if (expired){
      return false
    }

    val passwords_match = BCrypt.checkpw(auth_token, token_hash)
    println("The passwords match: " + passwords_match)
    passwords_match
  }

  /**Checks to see if the request contains cookies that correspond to a valid user*/
  def isAuthenticatedUserRequest(req: Request): Boolean = {
    val user_id = req.cookies.get_user_id()
    val token_id = req.cookies.get_token_id()
    val auth_token = req.cookies.get_auth_token()
    if(user_id == null || token_id == null || auth_token == null){
      return false
    }
    isValidToken(user_id.toInt, token_id, auth_token)
  }



}
