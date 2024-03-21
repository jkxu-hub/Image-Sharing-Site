package Backend.Models

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer, Map}
import Backend.Models.{Database_Updated => database_u, security => sec}
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import java.sql.Timestamp

/** Contains incoming cookies which are the cookies we receive in an HTTP request.
 * And out going cookies, which is a list of cookies we are adding in the response. */
class CookieProcessing(headers: Map[String, String]) {
  // A mapping of cookie name to cookie value sent in the HTTP request
  private val incoming_cookies: Map[String, String] = init_cookie_map(headers)
  //var incoming_cookie_directives: Map[String, Map[String, String]] = null

  // A list containing new and updated cookies to send in the response of Set-Cookies
  val outgoing_cookies: ListBuffer[String] = ListBuffer[String]()

  // A method that takes in a map of headers
  // Returns a map of updated cookies
  def init_cookie_map(headers: Map[String, String]): Map[String, String] = {
    if (headers.contains("Cookie")) {
      val cookie_str = headers("Cookie")
      put_cookies_in_map(cookie_str)
    } else {
      Map[String, String]()
    }
  }

  /** Puts the cookies from the Cookie header in an HTTP response into a scala map. */
  private def put_cookies_in_map(cookies: String): Map[String, String] = {
    val arr = cookies.split("; ")
    val cookie_map = Map[String, String]()
    for (elem <- arr) {
      val tup = elem.split("=")
      cookie_map(tup(0)) = tup(1)
    }
    cookie_map
  }

  /** Gets the number of times the user has visited the site */
  private def get_visits(): Int = {
    val contains_visits = incoming_cookies.contains("visits")
    if (!contains_visits) {
      return 0
    }
    incoming_cookies("visits").toInt
  }

  /** Increments the number of visits and adds the updated visit quantity to out return_cookies */
  def update_visits(): Int = {
    val visits = get_visits() + 1
    outgoing_cookies += ("visits=" + visits.toString)
    visits
  }


  /** Returns null if auth_token is not found. Otherwise returns auth_token */
  def get_auth_token(): String = {
    val contains_auth_token = incoming_cookies.contains("auth_token")
    if (!contains_auth_token) {
      return null
    }
    incoming_cookies("auth_token")
  }

  /** Returns null if user_id is not found. Otherwise returns user_id */
  def get_user_id(): String = {
    val contains_id = incoming_cookies.contains("user_id")
    if (!contains_id) {
      return null
    }
    incoming_cookies("user_id")
  }

  def get_token_id(): String = {
    val contains_id = incoming_cookies.contains("token_id")
    if (!contains_id) {
      return null
    }
    incoming_cookies("token_id")
  }

  /** Updates authentication_token and user_id if needed. Or creates new auth_token user_id, and token_id
   * cookies if needed. Updates the database if a new entry was created. */
  def updateAuthTokens(username: String): Unit = {
    val user_id = database_u.listAuthenticatedId(username)
    val cookie_token_id = get_token_id()
    val cookie_auth_token = get_auth_token()
    val cookies_exist = cookie_token_id != null && cookie_auth_token != null
    var cookies_are_valid = false
    if(cookies_exist){
      cookies_are_valid = sec.isValidToken(user_id,cookie_token_id, cookie_auth_token)
    }

    if(cookies_are_valid){
      //TODO do nothing since cookies are already set correctly? Just update the user_id

    }else{
      // Generate a new token
      //TODO improve this token generation process. Find a way to make tokens more random and unique.
      val authToken = sec.generateRandomToken()
      val (auth_token_hash, token_salt) = sec.generateHashAndSalt(authToken)
      val one_year_from_now: DateTime = DateTime.now.plusDays(365)
      val token_exp = new Timestamp(one_year_from_now.getMillis)

      //Formatting for token expiration directive
      val fmt = DateTimeFormat.forPattern("E dd MMM yyyy HH:mm:ss")
      val expiration_directive = one_year_from_now.toString(fmt) + " GMT"

      // Create a new DB entry
      val token_id = database_u.insertNewTokenEntry(user_id, username, auth_token_hash, token_salt, token_exp)
      assert(token_id != 0)

      val directives = "; Expires=" + expiration_directive + "; HttpOnly; SameSite=Strict" //TODO add directives as needed
      // Set Cookies based on new DB entry
      outgoing_cookies += ("user_id=" + user_id.toString) + directives
      outgoing_cookies += ("token_id=" + token_id) + directives
      outgoing_cookies += ("auth_token=" + authToken) + directives
    }
  }

  /** Sets all cookies related to authentication to expired.*/
  def logoutUpdateOutgoingCookies(): Unit = {
    val directives = "; Expires=Thu, 31 Oct 1982 07:28:00 GMT;; HttpOnly; SameSite=Strict"
    outgoing_cookies += ("user_id=" + "0") + directives
    outgoing_cookies += ("token_id=" + "0") + directives
    outgoing_cookies += ("auth_token=" + "deleted") + directives

    val cookie_user_id = get_user_id()
    val cookie_token_id = get_token_id()
    if (cookie_user_id == null || cookie_token_id == null) {
      return false
    }
  }

}