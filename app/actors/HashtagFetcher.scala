package actors

import actors.HashtagFetcher.{Tweet, HashtagTweets, CheckTweets}
import akka.actor.{ActorLogging, Actor}
import org.joda.time.DateTime
import scala.concurrent.duration._

class HashtagFetcher extends Actor with ActorLogging {

  val scheduler = context.system.scheduler.schedule(
    initialDelay = 5.seconds,
    interval = 10.minutes,
    receiver = self,
    message = CheckTweets
  )

  override def postStop(): Unit = {
    scheduler.cancel()
  }

  def checkTweets(): Unit = ???

  def storeTweets(usages: Seq[Tweet]): Unit = ???

  def receive: Receive = {
    case CheckTweets => checkTweets
    case HashtagTweets(usages) => storeTweets(usages)
  }
}

object HashtagFetcher {

  case object CheckTweets

  case class Tweet(id: String, created_at: DateTime, text: String, users: Seq[User])

  case class User(handle: String, id: String)

  case class HashtagTweets(usages: Seq[Tweet])

}