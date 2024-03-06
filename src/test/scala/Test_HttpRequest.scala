import Backend.Models.{Request}
import akka.util.ByteString
import org.scalatest.funsuite.AnyFunSuite

class Test_HttpRequest extends AnyFunSuite{
  var normal_post_request = ""
  normal_post_request += "POST /image-upload HTTP/1.1\r\n"
  normal_post_request += "X-Content-Type-Options: nosniff\r\n"
  normal_post_request += "Content-Length: 0\r\n"
  normal_post_request += "Content-Type: hello" + "\r\n"
  normal_post_request += "\r\n"
  normal_post_request += "DATA DATA DATA DATA DATA"

  var normal_get_request = ""
  normal_get_request += "GET / HTTP/1.1\r\n"
  normal_get_request += "Host: localhost:8001\r\n"
  normal_get_request += "Connection: keep-alive\r\n"
  normal_get_request += "\r\n"

  test("Request.parse_request_bytes") {
    val bytes: Array[Byte] = normal_post_request.getBytes()
    val req = new Request(ByteString(bytes))
    assert(req.requestLine === "POST /image-upload HTTP/1.1")
    assert(req.headers === "X-Content-Type-Options: nosniff\r\n" + "Content-Length: 0\r\n" +  "Content-Type: hello")
    assert(req.payload === "DATA DATA DATA DATA DATA".getBytes())
  }

  test("Request.parse_request_line") {
    val bytes: Array[Byte] = normal_post_request.getBytes()
    val req = new Request(ByteString(bytes))
    assert(req.method === "POST")
    assert(req.path === "/image-upload")
    assert(req.version === "HTTP/1.1")
  }


  test("Normal GET Request") {
    assert(3 === 3)
  }

  test("Normal POST Request") {
    assert(3 === 3)
  }

  test("Buffering POST Request") {
    assert(3 === 3)
  }
}


