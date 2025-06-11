package web.layers
import zio._
import zio.aws.core.config.AwsConfig
import zio.aws.netty.NettyHttpClient

object AwsLayers {
 
  val awsConfigLayer: ZLayer[Any, Throwable, AwsConfig] = NettyHttpClient.default >>> AwsConfig.default

}
