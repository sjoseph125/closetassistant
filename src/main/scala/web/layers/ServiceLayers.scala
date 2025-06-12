package web.layers

import zio.aws.dynamodb.DynamoDb
import AwsLayers.awsConfigLayer
import zio.ZLayer
import zio.aws.core.config.AwsConfig
import zio.aws.s3
import zio.aws.s3.S3
import zio.dynamodb.DynamoDBExecutor
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import zio.*

object ServiceLayers {
    lazy val dynamoDbLayer: ZLayer[AwsConfig, Throwable, DynamoDb] = awsConfigLayer >>> DynamoDb.live

    lazy val executor: ZLayer[AwsConfig, Throwable, DynamoDBExecutor] = dynamoDbLayer >>> DynamoDBExecutor.live

    lazy val s3PresignerLayer: ZLayer[AwsConfig, Throwable, S3Presigner] = awsConfigLayer >>> ZLayer.succeed(S3Presigner.builder().build())

    lazy val s3Layer: ZLayer[AwsConfig, Throwable, S3] = awsConfigLayer >>> S3.live

    lazy val ExecutorPresignerS3 = executor ++ s3PresignerLayer ++ s3Layer


    type ExecutorAndPresignerType = S3Presigner & DynamoDBExecutor

    type ExecutorAndS3Type = S3 & DynamoDBExecutor

    type ExecutorPresignerS3Type = S3Presigner & DynamoDBExecutor & S3
}
