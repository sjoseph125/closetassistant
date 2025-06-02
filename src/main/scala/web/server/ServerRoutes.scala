package web.server

import zio._
import zio.http._
import business.GetUserCloset
import web.layers.ServiceLayers
import java.security.Provider.Service
import zio.aws.dynamodb.DynamoDb
import zio.json.*
import zio.Exit.Success
import zio.Exit.Failure
import scala.util.chaining.*
import zio.dynamodb.DynamoDBExecutor

object ServerRoutes extends Flows {
  val routes: Routes[DynamoDBExecutor, Nothing] = Routes(
    Method.GET / Root -> handler(Response.text(s"Hello")),
    Method.GET / "v1" / "closet" / string("userId") -> handler {
      (userId: String, _: Request) =>
        getUserCloset(userId)
          .map { closet => Response.json(closet.toJson) }
        //   match
        //   case Success(value) => value
        //   case Failure(cause) => Response.fromCause(cause)
        //   case _ =>
        //     Response.fromThrowable(
        //       new Exception(s"Failed to greet user with unknown cause")
        //     )
        // )
        // .pipe(ZIO.succeed)
        // case _ => Response.fromThrowable(new Exception(s"Failed to greet user with unknown cause"))

        //.catchAll(err => ZIO.succeed(Response.text(s"Error: ${err.toString}")))
    }
  )
}
