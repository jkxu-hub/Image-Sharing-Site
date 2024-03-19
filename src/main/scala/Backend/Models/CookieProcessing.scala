package Backend.Models

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer, Map}
import Backend.Models.{security => sec}

object CookieProcessing {
  // A mapping of cookie name to cookie value sent in the HTTP request
  var cookie_map: Map[String, String] = Map[String, String]()

  // A list containing new and updated cookies to send in the response of Set-Cookies
  val set_cookies: ListBuffer[String] = ListBuffer[String]()

  // A method that takes in a map of headers
  // Returns an array of updated cookies
  def init_cookie_map(headers: Map[String, String]): Unit = {
    if(headers.contains("Cookie")){
      val cookie_str = headers("Cookie")
      cookie_map = put_cookies_in_map(cookie_str)
    }else{
      cookie_map = Map[String, String]()
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
    val contains_visits = cookie_map.contains("visits")
    if(!contains_visits){
      return 0
    }
    cookie_map("visits").toInt
  }

  /**Increments the number of visits and adds the updated visit quantity to out return_cookies*/
  def update_visits(): Int = {
    val visits = get_visits() + 1
    set_cookies += ("visits=" + visits.toString)
    visits
  }


  /** Returns null if auth_token is not found. Otherwise returns auth_token */
  def get_auth_token(): String = {
    val contains_auth_token = cookie_map.contains("auth_token")
    if(!contains_auth_token){
      return null
    }
    cookie_map("auth_token")
  }

  /** Returns null if user_id is not found. Otherwise returns user_id */
  def get_user_id(): String ={
    val contains_id = cookie_map.contains("user_id")
    if (!contains_id){
      return null
    }
    cookie_map("user_id")
  }

  /** Updates authentication_token and user_id if needed. Or creates new authentication_token and user_id
   * cookies if needed. Updates the database as well. */
  def updateAuthTokens(): Unit = {
    // database get token
    // database get Id
    // database get expiration
    val tokenIsValid = false //token ! = null and sys.time < database expiration
    if(tokenIsValid){
      //set_cookies += ("user_id=" + user_id)
      //set_cookies += ("auth_token=" + auth_token) + expiration + HTTPOnly
    }else{
      // generate new token
      // generate new sys.time
      // set tokens
      //update database
    }


  }

}
