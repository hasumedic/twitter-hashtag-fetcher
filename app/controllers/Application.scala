package controllers

import java.sql.Timestamp
import javax.inject.Inject

import database.DB
import org.joda.time.DateTime
import play.api.Configuration
import play.api.mvc.{Action, Controller}

class Application @Inject()(configuration: Configuration, database: DB) extends Controller {
  def index = Action.async { implicit request =>
    import generated.Tables._
    import org.jooq.impl.DSL._

    database.query { sql =>
      val tweetCount = sql.select(count())
        .from(TWEETS)
        .where(TWEETS.CREATED_ON.gt(value(new Timestamp(DateTime.now.minusDays(1).getMillis))))
        .fetchOne(count())

      Ok(views.html.index(tweetCount, configuration.getString("twitter.hashtag").getOrElse("")))
    }
  }
}
