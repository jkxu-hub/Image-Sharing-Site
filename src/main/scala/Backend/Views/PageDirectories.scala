package Backend.Views

object PageDirectories {
  private val frontend = "src/main/scala/Frontend/"
  val multiImageView_page: String = frontend + "MultiImageView/template.html"
  val home_page: String = frontend + "Home/index.html"
  val successfulFormSubmission_page: String = frontend + "SuccessfulFormSubmission/successfulFormSubmission.html"
  val globalChatPage: String = frontend + "GlobalChat/globalChat.html"

  val functions_js: String = frontend + "functions.js"
  val style_css: String = frontend + "style.css"
  val utf_txt: String = frontend + "utf.txt"
  val images: String = frontend + "MultiImageView/image"
  val clientPhotos: String = frontend + "clientPhotos"
  val globalChatFunctions_js: String = frontend + "GlobalChat/globalChatFunctions.js"

  val signup_html: String = frontend + "SignUp/signup.html"
  val signup_js: String = frontend + "SignUp/signup.js"
  val signupsuccess_html: String = frontend + "SignUp/signupSuccess.html"

  val login_html: String = frontend + "Login/login.html"
  val login_js: String = frontend + "Login/login.js"

}
