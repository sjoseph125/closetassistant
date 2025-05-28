package server
 
import zio._
import zio.http._

import zio.ZIOAppDefault
import core.GetUserCloset
import layers.AwsLayers
import layers.ServiceLayers


object HttpServer extends ZIOAppDefault {

  override def run = {
    val serverConfig = Server.Config.default.port(8080)


    (for {
      _ <- ZIO.logInfo("Starting HTTP server with AWS integration on port 8080...")
      // Server.serve can take Routes[Any, Response]
      _ <- Server.serve(ServerRoutes.routes)
    } yield ())
    .provide(
      // ZIO HTTP Server Layers
      ZLayer.succeed(serverConfig),
      Server.live,
      ServiceLayers.dynamoDbLayer
    )
    .catchAllDefect(defect =>
      println(defect.toString)
      ZIO.logErrorCause("Server crashed with defect at the highest level", Cause.fail(defect)) *>
      ZIO.succeed(ExitCode.failure) // Ensure the app exits on critical unhandled defects
    )
  }
}