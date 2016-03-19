package actors

import java.sql.Timestamp
import java.util.Locale
import javax.inject._

import actors.HashtagFetcher.{CheckTweets, HashtagTweets, Tweet, User}
import akka.actor.{Actor, ActorLogging}
import akka.pattern.pipe
import com.google.inject.AbstractModule
import database.DB
import generated.Tables._
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.jooq.impl.DSL._
import play.api.Configuration
import play.api.libs.concurrent.AkkaGuiceSupport
import play.api.libs.json.JsArray
import play.api.libs.oauth.{ConsumerKey, OAuthCalculator, RequestToken}
import play.api.libs.ws.WSClient

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.control.NonFatal

class HashtagFetcher @Inject()(configuration: Configuration, database: DB, WS: WSClient) extends Actor with ActorLogging {

  implicit val executionContext = context.dispatcher

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

  var lastTweetTime: Option[DateTime] = Some(DateTime.now)


  def checkTweets(): Unit = {

    val tweetsCollection = for {
      (consumerKey, requestToken) <- credentials
      time <- lastTweetTime
    } yield fetchTweets(consumerKey, requestToken, configuration.getString("twitter.hashtag").get, time)

    tweetsCollection.foreach { tweets =>
      tweets.map { t =>
        HashtagTweets(t)
      } recover { case NonFatal(t) =>
        log.error(t, "Could not fetch tweets")
        HashtagTweets(Seq.empty)
      } pipeTo self
    }
  }

  def fetchTweets(consumerKey: ConsumerKey, requestToken: RequestToken, hashtag: String, time: DateTime): Future[Seq[Tweet]] = {
    val dateFormat = DateTimeFormat.forPattern("EEE MMM dd HH:mm:ss Z yyyy").withLocale(Locale.ENGLISH)

    WS.url("https://api.twitter.com/1.1/search/tweets.json")
      .sign(OAuthCalculator(consumerKey, requestToken))
      .withQueryString("q" -> s"#$hashtag")
      .get()
      .map { response =>
        val tweets = (response.json \ "statuses").as[JsArray].value.map { status =>
          val id = (status \ "id_str").as[String]
          val user = User((status \ "user" \ "id_str").as[String], (status \ "user" \ "screen_name").as[String])
          val text = (status \ "text").as[String]
          val created_at = dateFormat.parseDateTime((status \ "created_at").as[String])
          Tweet(id, user, text, created_at)
        }
        tweets.filter(_.created_at.isAfter(time))
      }
  }

  def storeTweets(tweets: Seq[Tweet]) = database.withTransaction { sql =>
    log.info("Inserting potentially {} tweets into the database", tweets.size)
    val now = new Timestamp(DateTime.now.getMillis)

    def upsertUser(handle: String) = {
      sql.insertInto(TWITTER_USER, TWITTER_USER.TWITTER_USERNAME, TWITTER_USER.CREATED_ON)
        .select(
          select(value(handle), value(now))
            .whereNotExists(
              selectOne()
                .from(TWITTER_USER)
                .where(TWITTER_USER.TWITTER_USERNAME.equal(handle))
            )
        )
        .execute()
    }

    tweets.foreach { tweet =>
      upsertUser(tweet.user.handle)

      sql.insertInto(TWEETS, TWEETS.TWEET_ID, TWEETS.USER_ID, TWEETS.TEXT, TWEETS.CREATED_ON)
        .select(
          select(
            value(tweet.id),
            TWITTER_USER.ID,
            value(tweet.text),
            value(now)
          )
            .from(TWITTER_USER)
            .where(TWITTER_USER.TWITTER_USERNAME.equal(tweet.user.handle))
            .andNotExists(
              selectOne()
                .from(TWEETS)
                .where(TWEETS.TWEET_ID.equal(tweet.id))
            )
        )
        .execute()
    }
  }

  def receive: Receive = {
    case CheckTweets => checkTweets()
    case HashtagTweets(usages) => storeTweets(usages)
  }
}

object HashtagFetcher {

  case object CheckTweets

  case class Tweet(id: String, user: User, text: String, created_at: DateTime)

  case class User(id: String, handle: String)

  case class HashtagTweets(usages: Seq[Tweet])

}

class HashtagFetcherModule extends AbstractModule with AkkaGuiceSupport {
  def configure(): Unit = bindActor[HashtagFetcher]("fetcher")
}