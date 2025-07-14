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
import web.layers.ServiceLayers.*
import core.*
import web.externalservices.*
import web.server.ExternalSvcFlows.*
import zio.aws.s3.S3

trait Flows extends Config {

  lazy val getUserCloset: (
      String,
      Boolean
  ) => URIO[ExecutorAndPresignerType, Option[UserCloset]] =
    (userId, includeMetaData) =>
      new GetUserClosetSvcFlow(
        GetUserClosetSvcFlow.CfgCtx(
          getClosetData = getClosetData,
          getClosetItem = getClosetItem,
          getPresignedUrls = getPresignedUrls,
          addNewCloset = addNewCloset
        )
      )(userId, includeMetaData)

  lazy val updateUserCloset
      : UpdateUserCloset => RIO[ClientAndS3 & ExecutorAndPresignerType, Option[
        UserCloset
      ]] = updateUserCloset =>
    new UpdateUserClosetSvcFlow(
      UpdateUserClosetSvcFlow.CfgCtx(
        getClosetData = getClosetData,
        addClosetItem = addClosetItem,
        updateClosetData = updateClosetData,
        getUserCloset = getUserCloset,
        deleteClosetItem = deleteClosetItem,
        llmInferenceFlow = llmPostRequestAddItem,
        addNewCloset = addNewCloset
      )
    )(updateUserCloset)

  lazy val getPresignedUrls: GetPresignedURLRequest => RIO[
    ExecutorAndPresignerType,
    GetPresignedURLResponse
  ] = request =>
    new GetPresignedURLSvcFlow(
      GetPresignedURLSvcFlow.CfgCtx(
        getClosetData = getClosetData,
        bucketName = bucketName
      )
    )(request)

  lazy val recommendOutfit: SearchRequest => RIO[
    ExecutorAndPresignerType & Client,
    SearchResponse
  ] = request =>
    new RecommendOutfitSvcFlow(
      RecommendOutfitSvcFlow.CfgCtx(
        llmSearchOutfits = llmSearchOutfits,
        getUserCloset = getUserCloset,
        weatherInfoFlow = weatherInfoFlow
      )
    )(request)
}

object DBFlows extends Config {
  lazy val getClosetData: (
      KeyConditionExpr[UserClosetModel]
  ) => ZIO[DynamoDBExecutor, Throwable, Chunk[UserClosetModel]] =
    (keyCondition) =>
      DynamoDBQueries
        .queryAll[UserClosetModel](closetDataTableName, keyCondition)

  lazy val getClosetItem: (
      KeyConditionExpr.PrimaryKeyExpr[ClosetItemModel]
  ) => DynamoDBQuery[ClosetItemModel, Either[ItemError, ClosetItemModel]] =
    keyCondition =>
      DynamoDBQueries.get[ClosetItemModel](closetItemTableName)(keyCondition)

  lazy val updateClosetData: (
      PrimaryKeyExpr[UserClosetModel],
      Action[UserClosetModel]
  ) => ZIO[DynamoDBExecutor, DynamoDBError, Option[UserClosetModel]] =
    (key, action) =>
      DynamoDBQueries.update[UserClosetModel](closetDataTableName, key, action)

  lazy val addClosetItem
      : ClosetItemModel => DynamoDBQuery[ClosetItemModel, Option[
        ClosetItemModel
      ]] =
    item => DynamoDBQueries.put[ClosetItemModel](closetItemTableName, item)

  lazy val addNewCloset
      : UserClosetModel => DynamoDBQuery[UserClosetModel, Option[UserClosetModel]] = closet =>
    DynamoDBQueries.put[UserClosetModel](closetDataTableName, closet)

  lazy val deleteClosetItem: PrimaryKeyExpr[ClosetItemModel] => DynamoDBQuery[
    ClosetItemModel,
    Option[ClosetItemModel]
  ] =
    key => DynamoDBQueries.deleteItem[ClosetItemModel](closetItemTableName, key)
}

object ExternalSvcFlows extends Config {
  lazy val llmPostRequestAddItem: PerformInference => RIO[ClientAndS3, Map[
    String,
    LLMInferenceResponse
  ]] = request =>
    new LLMInferneceAddItemsFlow(
      LLMInferneceAddItemsFlow.CfgCtx(
        apiUrl = llmApiUrl,
        s3Download = s3Download,
        model = model,
        prompt = itemMetadataPrompt
      )
    ).postRequest(request)

  lazy val llmSearchOutfits: String => RIO[Client, LLMInferenceResponse] =
    request =>
      new LLMInferenceSearchOutfitsFlow(
        LLMInferenceSearchOutfitsFlow.CfgCtx(
          apiUrl = llmApiUrl,
          model = model,
          prompt = userSearchPrompt
        )
      )(request)

  lazy val s3Download: String => URIO[S3, Chunk[Byte]] = imageIdentifier =>
    new S3DownloadFlow(
      S3DownloadFlow.CfgCtx(
        bucketName = bucketName
      )
    )(imageIdentifier)

  lazy val weatherInfoFlow: (
      Location
  ) => RIO[Client, WeatherInfoResponse] = userLocation =>
    new WeatherInfoFlow(
      WeatherInfoFlow.CfgCtx(
        apiUrl = weatherApiUrl,
        apiKey = weatherApiKey
      )
    )(userLocation)
}