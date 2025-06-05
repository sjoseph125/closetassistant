package web.layers

import zio.aws.netty.NettyHttpClient._
import zio._
import zio.aws.netty.NettyHttpClient
import software.amazon.awssdk.http.async.SdkAsyncHttpClient
import zio.aws.core.httpclient.HttpClient
import zio.aws.core.config.AwsConfig

object AwsLayers {
 
  val awsConfigLayer: ZLayer[Any, Throwable, AwsConfig] = NettyHttpClient.default >>> AwsConfig.default

}
