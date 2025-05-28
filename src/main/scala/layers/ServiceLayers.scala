package layers

import zio.aws.dynamodb.DynamoDb
import AwsLayers.awsConfigLayer
import zio.ZLayer
import zio.aws.core.config.AwsConfig

object ServiceLayers {
    lazy val dynamoDbLayer: ZLayer[Any, Throwable, DynamoDb] =
     awsConfigLayer >>> DynamoDb.live
}
