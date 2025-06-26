package utils

import zio.http.{Body, Response, Status}
import zio.*
import core.BaseRequest
import zio.json.*
import scala.util.chaining.*
import utils.Extensions.toJsonResponse
import scala.util.Success
import scala.util.Failure
import zio.dynamodb.DynamoDBExecutor
import utils.Extensions.toHttpResponse
import zio.http.Header

object Extensions {
//   extension (body: Body)
//     def asStringWith(
//         f: BaseRequest => ZIO[Any, Throwable, Response]
//     ): ZIO[Any, Throwable, Response] = body.asString
//       .flatMap { jsonString =>
//         jsonString.fromJson[BaseRequest] match {
//           case Right(newItem) =>
//             f(newItem)
//               .map(res => Response.json(res.toJson))
//           case Left(error) =>
//             Response(
//               status = Status.BadRequest,
//               body = Body.fromString(s"Invalid JSON: ${error}")
//             ).pipe(ZIO.succeed)
//         }
//       }
//       .foldCause(
//         cause =>
//           Response(
//             status = Status.InternalServerError,
//             body =
//               Body.fromString(s"Error processing request: ${cause.prettyPrint}")
//           ),
//         response => response
//       )

  extension [A](a: A)
    def toJsonResponse(using encoder: JsonEncoder[A]): Response =
      Response.json(a.toJson).addHeader(Header.AccessControlAllowOrigin.All)

  extension [R, E, A](zio: ZIO[R, E, A])
    def toHttpResponse(using encoder: JsonEncoder[A]): ZIO[R, Nothing, Response] =
      zio.fold(
        e =>
          Response(
            status = Status.InternalServerError,
            body = Body.fromString(s"Failed to excecute request with error: ${e.toString}")
          ),
        a => a.toJsonResponse
      )     
}
