package core 
import zio._

class GetUserCloset extends (String => Task[String]) {
  override def apply(userId: String): Task[String] = {
    // Simulate fetching user closet data
    ZIO.succeed(s"Closet data for user $userId")
  }
  
}
