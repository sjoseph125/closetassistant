package server
 
import zio._
import zio.http._

import zio.ZIOAppDefault
import core.GetUserCloset


object HttpServer extends ZIOAppDefault with Flows {


  val routes = Routes(
    Method.GET / Root -> handler(Response.text(s"Hello")),
    Method.GET / "greet" / string("name") -> handler { (name: String, req: Request) =>
      val userId = if (name.isEmpty()) "No name provided" else name
      getUserCloset("samson")
        .map(closet => Response.json(s"""{result: "Greeting user with ID: $userId, Closet: $closet"}"""))
        .catchAll(err => ZIO.fail(Response.text(s"Error: ${err.getMessage}")))
    }
  )

  def run = Server.serve(routes).provide(Server.default)
}