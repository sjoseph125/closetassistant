package web.resources

abstract class Config {
    lazy val closetDataTableName = "arn:aws:dynamodb:us-east-1:288761768209:table/closet_data"
    lazy val closetItemTableName = "arn:aws:dynamodb:us-east-1:288761768209:table/closet_items"
    lazy val bucketName = "closet-assistant-image-repository"
    lazy val llmApiUrl = "https://lcd4hyd7tm06pb-11434.proxy.runpod.net/api/generate"
    lazy val model = "gemma3:12b"
    lazy val prompt = "Describe this clothing. Tell me about the color, the fabric, what activities one would wear it to and what season it will be appropriate for. The list of activities must be exahaustive"
}
