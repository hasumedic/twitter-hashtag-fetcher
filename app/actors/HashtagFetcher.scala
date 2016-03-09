package actors

import actors.HashtagFetcher.{Tweet, HashtagTweets, CheckTweets}
import akka.actor.{ActorLogging, Actor}
import org.joda.time.DateTime
import play.api.Configuration
import play.api.libs.oauth.{RequestToken, ConsumerKey}
import scala.concurrent.duration._
import javax.inject._

class HashtagFetcher @Inject()(configuration: Configuration) extends Actor with ActorLogging {

  val scheduler = context.system.scheduler.schedule(
    initialDelay = 5.seconds,
    interval = 10.minutes,
    receiver = self,
    message = CheckTweets
  )

  def credentials = for {
    apiKey <- configuration.getString("twitter.apiKey")
    apiSecret <- configuration.getString("twitter.apiSecret")
    token <- configuration.getString("twitter.accessToken")
    tokenSecret <- configuration.getString("twitter.accessTokenSecret")
  } yield (ConsumerKey(apiKey, apiSecret), RequestToken(token, tokenSecret))

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