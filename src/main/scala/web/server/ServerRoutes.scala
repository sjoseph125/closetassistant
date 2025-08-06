package web.server
import zio._
import zio.http._
import zio.http.Method.*
import zio.json.*
import core.{UpdateUserCloset, SearchRequest, GetPresignedURLRequest}
import utils.Extensions.*
import web.layers.ServiceLayers.ExecutorPresignerS3Type
import scala.util.chaining.scalaUtilChainingOps

object ServerRoutes extends Flows {
  val routes: Routes[ExecutorPresignerS3Type & Client, Nothing] =
    Routes(
      GET / Root -> handler(Response.text(s"Hello")),
      GET / "v1" / "closet" / string("userId") -> handler {
        (userId: String, _: Request) => getUserCloset(userId, false).toHttpResponse
      },
      GET / "v1" / "upload" / string("userId") -> handler {
        (userId: String,  req: Request) =>
          getPresignedUrls(GetPresignedURLRequest(userId = userId, numOfUrls = req.queryParameters.getAll("numOfUrls").headOption)).toHttpResponse
      },
      PUT / "v1" / "closet" -> handler { (request: Request) =>
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
                case Right(newItems) =>
                  updateUserCloset(newItems).toHttpResponse
                case Left(error) =>
                  Response(
                    status = Status.BadRequest,
                    body = Body.fromString(s"Invalid JSON: ${error}")
                  ).pipe(ZIO.succeed)
              }
          )

      },
      
      DELETE / "v1" / "closet" -> handler { (request: Request) =>
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
      ,
      Method.POST / "v1" / "recommend-outfit" -> handler { (request: Request) =>
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
              body.fromJson[SearchRequest] match {
                case Right(searchOutfitReq) =>
                  recommendOutfit(searchOutfitReq).toHttpResponse
                case Left(error) =>
                  Response(
                    status = Status.BadRequest,
                    body = Body.fromString(s"Invalid JSON: ${error}")
                  ).pipe(ZIO.succeed)
              }
          )
      }
    )
}
