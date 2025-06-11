package web.server
import zio.*
import zio.dynamodb.*
import zio.dynamodb.KeyConditionExpr.PrimaryKeyExpr
import zio.dynamodb.UpdateExpression.Action
import zio.dynamodb.DynamoDBError.ItemError
import zio.dynamodb.DynamoDBExecutor
import zio.http.*
import persistence.models.*
import persistence.queries.DynamoDBQueries
import business.*
import web.server.DBFlows.*
import web.resources.Config
import web.layers.ServiceLayers.ExecutorAndPresignerType
import core.*
import web.externalservices.LLMInferneceSvcFlow
import web.server.ExternalSvcFlows.llmPostRequest

trait Flows extends Config{

  lazy val getUserCloset: String => URIO[DynamoDBExecutor, Option[UserCloset]] = userId =>
    new GetUserClosetSvcFlow(
      GetUserClosetSvcFlow.CfgCtx(
        getClosetData = getClosetData,
        getClosetItem = getClosetItem
      )
    )(userId)

  lazy val updateUserCloset: UpdateUserCloset => RIO[DynamoDBExecutor, Option[UserCloset]] = updateUserCloset =>
    new UpdateUserClosetSvcFlow(
      UpdateUserClosetSvcFlow.CfgCtx(
        getClosetData = getClosetData,
        addClosetItem = addClosetItem,
        updateClosetData = updateClosetData,
        getUserCloset = getUserCloset,
        deleteClosetItem = deleteClosetItem
      )
    )(updateUserCloset)

  lazy val getPresignedUrl: String => RIO[ExecutorAndPresignerType, GetPresignedURL] = userId =>
    new GetPresignedURLSvcFlow(
      GetPresignedURLSvcFlow.CfgCtx(
        getClosetData = getClosetData,
        bucketName = bucketName
      )
    )(userId)
    
  lazy val recommendOutfit: SearchRequest => RIO[Client, SearchResponse] = request =>
    new RecommendOutfitSvcFlow(
      RecommendOutfitSvcFlow.CfgCtx(
        inferSearchRequest = llmPostRequest
      )
    )(request)
}

object DBFlows extends Config {
  lazy val getClosetData: (KeyConditionExpr[UserClosetModel]) => ZIO[DynamoDBExecutor, Throwable, Chunk[UserClosetModel]] = 
    (keyCondition) => DynamoDBQueries.queryAll[UserClosetModel](closetDataTableName, keyCondition)
    
  lazy val getClosetItem: (KeyConditionExpr.PrimaryKeyExpr[ClosetItemModel]) => DynamoDBQuery[ClosetItemModel, Either[ItemError, ClosetItemModel]] = 
    keyCondition => DynamoDBQueries.get[ClosetItemModel](closetItemTableName)(keyCondition)

  lazy val updateClosetData: (PrimaryKeyExpr[UserClosetModel], Action[UserClosetModel]) => ZIO[DynamoDBExecutor, DynamoDBError, Option[UserClosetModel]] = 
    (key, action) => DynamoDBQueries.update[UserClosetModel](closetDataTableName, key, action)
    
  lazy val addClosetItem: ClosetItemModel => DynamoDBQuery[ClosetItemModel, Option[ClosetItemModel]] = 
    item => DynamoDBQueries.put[ClosetItemModel](closetItemTableName, item)
    
  lazy val deleteClosetItem: PrimaryKeyExpr[ClosetItemModel] => DynamoDBQuery[ClosetItemModel, Option[ClosetItemModel]] = 
    key => DynamoDBQueries.deleteItem[ClosetItemModel](closetItemTableName, key)
}

object ExternalSvcFlows extends Config {
  lazy val llmPostRequest: SearchRequest => RIO[Client, Response] = request => 
    new LLMInferneceSvcFlow(
      LLMInferneceSvcFlow.CfgCtx(
        apiUrl = llmApiUrl
      )
    ).postRequest(request)
}