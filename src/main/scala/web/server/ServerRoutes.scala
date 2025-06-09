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
import utils.Extensions.*

object ServerRoutes extends Flows {
  val routes: Routes[S3Presigner & DynamoDBExecutor, Nothing] = Routes(
    Method.GET / Root -> handler(Response.text(s"Hello")),
    Method.GET / "v1" / "closet" / string("userId") -> handler {
      (userId: String, _: Request) => getUserCloset(userId).toHttpResponse
    },
    Method.GET / "v1" / "upload" / string("userId") -> handler {
      (userId: String, _: Request) => getPresignedUrl(userId).toHttpResponse
    },
    Method.PUT / "v1" / "closet" -> handler { (request: Request) =>
      request.body.asString
        .foldZIO(
          cause =>
            Response(
              status = Status.InternalServerError,
              body = Body.fromString(
                s"Error reading request body: ${cause.getMessage()}"
              )
            ).pipe(ZIO.succeed),
          body =>
            body.fromJson[UpdateUserCloset] match {
              case Right(newItems) => updateUserCloset(newItems).toHttpResponse
              case Left(error) =>
                Response(
                  status = Status.BadRequest,
                  body = Body.fromString(s"Invalid JSON: ${error}")
                ).pipe(ZIO.succeed)
            }
        )

    },
    Method.DELETE / "v1" / "closet" -> handler { (request: Request) =>
      request.body.asString
        .foldZIO(
          cause =>
            Response(
              status = Status.InternalServerError,
              body = Body.fromString(
                s"Error reading request body: ${cause.getMessage()}"
              )
            ).pipe(ZIO.succeed),
          body =>
            body.fromJson[UpdateUserCloset] match {
              case Right(deleteItems) =>
                updateUserCloset(
                  deleteItems.copy(deleteItems = true)
                ).toHttpResponse
              case Left(error) =>
                Response(
                  status = Status.BadRequest,
                  body = Body.fromString(s"Invalid JSON: ${error}")
                ).pipe(ZIO.succeed)
            }
        )
    }
    // ,
    // Method.POST / "v1" / "recommend-outfit" -> handler {
    //   (userId: String, closetItemKey: String, _: Request) =>
    //     ZIO.logInfo(s"Delete request for user $userId") *>
    //       ZIO.succeed(Response.text(s"Delete request for user $userId"))
    // }
  )
}
