import org.scalatest.funsuite.AnyFunSuite
import Backend.Models.{Database_Updated => database_u}
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import java.util.Locale
import scala.util.Random
import java.sql.Timestamp
import java.time.LocalDateTime



class Test_Database extends AnyFunSuite {
  def generate_random_alphabet_seq(len: Int): String = {
    Random.alphanumeric.filter(_.isLetter).take(len).mkString

  }

  def generate_random_digit_seq(): Int = {
    // Generate a random 2-digit number
    Random.nextInt(90) + 10
  }

  test("Database Datetime comparison") {
    // Get the current date and time
    val currentDateTime = DateTime.now()
    // Add 365 days to the current date and time
    val few_months_from_now: DateTime = currentDateTime.plusDays(364)
    val one_year_from_now: DateTime = currentDateTime.plusDays(365)
    val one_year_one_day: DateTime = currentDateTime.plusDays(366)

    val user_id = generate_random_digit_seq()
    val username = generate_random_alphabet_seq(5)
    val auth_token_hash = generate_random_alphabet_seq(5)
    val token_salt = generate_random_alphabet_seq(5)
    val token_exp = new Timestamp(one_year_from_now.getMillis)

    val token_id = database_u.insertNewTokenEntry(user_id, username, auth_token_hash, token_salt, token_exp)
    println("My token_id: " + token_id)
    /*TODO token id is hard coded for now*/
    val token_expiration = database_u.listTokenExpiration(user_id, token_id)
    println(token_expiration)
    println(one_year_one_day)

    val fmt = DateTimeFormat.forPattern("E dd MMM yyyy HH:mm:ss")
    val directive = one_year_from_now.toString(fmt)+ " GMT"
    println(directive)

    assert(one_year_one_day.isAfter(token_expiration.getTime))
    assert(few_months_from_now.isBefore(token_expiration.getTime))
    //Insert into database
    // compare listTokenExpiration to the current time
  }

  /*TODO write a test for concurrent insertNewTokenEntry*/
}

