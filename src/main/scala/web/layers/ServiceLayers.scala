package web.layers

import zio.aws.dynamodb.DynamoDb
import AwsLayers.awsConfigLayer
import zio.ZLayer
import zio.aws.core.config.AwsConfig
import zio.aws.s3
import zio.aws.s3.S3
import zio.dynamodb.DynamoDBExecutor

object ServiceLayers {
    lazy val dynamoDbLayer: ZLayer[Any, Throwable, DynamoDb] = awsConfigLayer >>> DynamoDb.live

    lazy val executor = dynamoDbLayer >>> DynamoDBExecutor.live

    lazy val s3Layer: ZLayer[Any, Throwable, S3] = awsConfigLayer >>> s3.S3.live

    //  val s3ConfigLayer = awsConfigLayer >>> 
}
