package server

import zio._
import zio.http._
import zio.Task
import core._
import zio.aws.dynamodb.DynamoDb
import zio.aws.core.AwsError
import layers.ServiceLayers

trait Flows {
  lazy val getUserCloset: String => ZIO[DynamoDb, AwsError | Exception, String] = userId =>
    new GetUserCloset()
      .apply(userId)
      .provide(
        ServiceLayers.dynamoDbLayer
          .mapError {
            case t: Throwable => new Exception(t)
          }
      )
    //   .map(userId => Response.text(s"Greeting user with ID: $userId"))
    //   .catchAll(err => ZIO.succeed(Response.text(s"Error: ${err.getMessage}")))
}

// class FlowsBoot extends Flows {

//   override def getUserCloset: String => Task[String] = userId =>
//     new GetUserCloset().apply(userId)
//         // .map(userId => Response.text(s"Greeting user with ID: $userId"))
//         // .catchAll(err => Zio.succeed(Response.text(s"Error: ${err.getMessage}")))
// }
