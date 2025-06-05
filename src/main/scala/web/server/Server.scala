package web.server

import zio._
import zio.http.*
import zio.json.*

import zio.ZIOAppDefault
import web.layers.ServiceLayers
import web.layers.AwsLayers

object HttpServer extends ZIOAppDefault {

  import ZIO._

  override def run = {
    val serverConfig = Server.Config.default
      .port(8080)
      .keepAlive(true)
      .idleTimeout(30.seconds)
      

    for {
      _ <- ZIO.logInfo(
        "Starting HTTP server with AWS integration on port 8080..."
      )
      // Server.serve can take Routes[Any, Response]
      _ <- Server
        .serve(ServerRoutes.routes)
        .provide(
          // ZIO HTTP Server Layers
          ZLayer.succeed(serverConfig),
          Server.live,
          AwsLayers.awsConfigLayer,
          ServiceLayers.executor,
          ServiceLayers.s3Layer
        )
        .catchAllDefect(defect =>
          logErrorCause(
            "Server crashed with defect at the highest level",
            Cause.fail(defect)
          ) *>
            succeed(
              ExitCode.failure
            ) // Ensure the app exits on critical unhandled defects
        )
    } yield ()

  }
}
