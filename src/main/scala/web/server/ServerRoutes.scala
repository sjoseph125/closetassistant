package web.server

import zio._
import zio.http._
import business.GetUserClosetSvcFlow
import web.layers.ServiceLayers
import java.security.Provider.Service
import zio.aws.dynamodb.DynamoDb
import zio.json.*
import zio.Exit.Success
import zio.Exit.Failure
import scala.util.chaining.*
import zio.dynamodb.DynamoDBExecutor
import core.UpdateUserCloset
import software.amazon.awssdk.services.s3.presigner.S3Presigner

object ServerRoutes extends Flows {
  val routes: Routes[S3Presigner & DynamoDBExecutor, Nothing] = Routes(
    Method.GET / Root -> handler(Response.text(s"Hello")),
    Method.GET / "v1" / "closet" / string("userId") -> handler {
      (userId: String, _: Request) =>
        getUserCloset(userId)
          .map { closet => Response.json(closet.toJson) }
    },
    Method.GET / "v1" / "upload" / string("userId") -> handler{
      (userId: String, _: Request) =>
        getPresignedUrl(userId)
        .foldZIO(
          error => ZIO.succeed(Response(
            status = Status.InternalServerError,
            body = Body.fromString(s"Error generating presigned URL: ${error.getMessage}")
          )),
          url => ZIO.succeed(Response.text(url.toString))
        )
        //ZIO.succeed(Response.text(s"Upload closet for user: $userId"))
    },
    Method.PUT / "v1" / "closet" -> handler {
      (request: Request) => request.body.asString.flatMap { jsonString =>
        jsonString.fromJson[UpdateUserCloset] match {
          case Right(newItem) => updateUserCloset(newItem)
              .map( _ => Response.status(Status.NoContent))
          case Left(error) =>
            ZIO.succeed(Response(
              status = Status.BadRequest,
              body = Body.fromString(s"Invalid JSON: ${error}")
            ))
        }

      }.foldCause(
        cause => Response(
          status = Status.InternalServerError,
          body = Body.fromString(s"Error processing request: ${cause.prettyPrint}")
        ),
        response => response
      )
        
    }
  )
}
