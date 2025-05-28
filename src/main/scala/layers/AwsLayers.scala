package layers

import zio.aws.netty.NettyHttpClient._
import zio._
import zio.aws.netty.NettyHttpClient
import software.amazon.awssdk.http.async.SdkAsyncHttpClient
import zio.aws.core.httpclient.HttpClient
import zio.aws.core.config.AwsConfig

object AwsLayers {
  val httpClientLayer: ZLayer[Any, Throwable, HttpClient] = NettyHttpClient.default

  val awsConfigLayer: ZLayer[Any, Throwable, AwsConfig] = httpClientLayer >>> AwsConfig.default

}
