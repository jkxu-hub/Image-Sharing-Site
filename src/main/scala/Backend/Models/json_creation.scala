package Backend.Models

import java.sql.ResultSet

object json_creation {
  def user_entry_string(user: ResultSet): String = {
    user.next()
    val user_id = user.getString("user_id")
    val username = user.getString("username")
    val email = user.getString("email")
    val json_str = ujson.write(ujson.Obj("user_id" -> user_id, "username" -> username, "email" -> email))
    json_str

  }

}
