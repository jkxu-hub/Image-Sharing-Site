package Backend.Models
import akka.util.ByteString
import org.mindrot.jbcrypt.BCrypt

import scala.util.Random
import Backend.Models.{Database_Updated => database_u}

/** Does not hold any state, just contains helper methods pertaining to security */
object security {


  /**  Sanitizes user input that is meant to be embedded in html the server sends*/
  def htmlInjectionReplace(str: String): String = {
    // TODO could be more efficient if you loop through the string once instead of multiple times
    str.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
  }


  /**Generates a hash and salt for a given password or authentication token.
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

  def isValidToken(user_id: String, auth_token:String): Boolean = {
    //database get token
    // database get expiration
    // if the auth_token matches the auth_token at user_id true, otherwise false
    false
  }



}
