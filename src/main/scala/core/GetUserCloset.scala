package core 
import zio._
import zio.aws.dynamodb.DynamoDb
import zio.aws.dynamodb.model._
import zio.prelude.data.Optional
import zio.aws.dynamodb.model.primitives._
import zio.dynamodb.ToAttributeValue

import zio.dynamodb.AttrMap
import zio.aws.dynamodb.model.primitives.StringAttributeValue
import zio.aws.core.AwsError
// import zio.dynamodb.AttrMap
// import software.amazon.awssdk.services.dynamodb.model.{AttributeValue => SdkAttributeValue} // AWS SDK's AttributeValue

class GetUserCloset extends (String => ZIO[DynamoDb, AwsError | Exception, String]) {
  override def apply(userId: String): ZIO[DynamoDb, AwsError | Exception, String] = 
    // Simulate fetching user closet data
    import zio.aws.dynamodb.model.primitives.TableName


    val getItemRequest = GetItemRequest(
      tableName = TableArn("arn:aws:dynamodb:us-east-1:288761768209:table/closet_data"),
      key =  Map(AttributeName("closet_data_key") -> AttributeValue(StringAttributeValue(userId))))
    ZIO.serviceWithZIO[DynamoDb] { dynamoDb =>
      dynamoDb.getItem(getItemRequest).flatMap { response =>
        response.item match {
          case Optional.Present(item) =>
            ZIO.succeed(item.get(AttributeName("num_of_items")) match {
              case Some(value) => value.n.getOrElse(throw new Exception("No number of items found"))
              case None => "No items found in closet"
            })
          case Optional.Absent =>
            ZIO.fail(new Exception(s"No closet data found for user $userId"))
        }
      }
    }
}
