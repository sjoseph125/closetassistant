package core

import zio.json.{JsonEncoder, JsonDecoder, DeriveJsonDecoder}


sealed trait BaseRequest

abstract class BaseRespose (
    val a: String,
    val b: String
)


// object BaseRequest {

//   implicit val decoder: JsonDecoder[BaseRequest] = DeriveJsonDecoder.gen[BaseRequest]
// }

