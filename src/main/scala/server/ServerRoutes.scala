package server

import zio._
import zio.http._
import core.GetUserCloset
import layers.ServiceLayers
import java.security.Provider.Service

object ServerRoutes extends Flows {
  val routes = Routes(
    Method.GET / Root -> handler(Response.text(s"Hello")),
    Method.GET / "greet" / string("name") -> handler { (name: String, req: Request) =>
      
      val userId = if (name.isEmpty()) "No name provided" else name
      getUserCloset(name)
      //   .provide(
      //   ServiceLayers.dynamoDbLayer.mapError {
      //     case t: Throwable => new Exception(t)
      //   }
      // )
        .map(closet => Response.json(s"""{result: "Greeting user with ID: $userId, Closet: $closet"}"""))
        .catchAll(err => ZIO.succeed(Response.text(s"Error: ${err.toString}")))
    }
  )
}