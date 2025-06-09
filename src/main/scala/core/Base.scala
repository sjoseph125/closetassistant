package core

import zio.json.{JsonEncoder, JsonDecoder, DeriveJsonDecoder}


sealed trait BaseRequest

trait BaseRespose

// object BaseRequest {

//   implicit val decoder: JsonDecoder[BaseRequest] = DeriveJsonDecoder.gen[BaseRequest]
// }

