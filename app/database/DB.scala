package database

import javax.inject.Inject

import akka.actor.ActorSystem
import org.jooq.impl.DSL
import org.jooq.{SQLDialect, DSLContext}
import play.api.db.Database

import scala.concurrent.{Future, ExecutionContext}

class DB @Inject()(db: Database, system: ActorSystem) {
  val databaseContext: ExecutionContext = system.dispatchers.lookup("contexts.database")

  def query[A](block: DSLContext => A): Future[A] = Future {
    db.withConnection { connection =>
      val sql = DSL.using(connection, SQLDialect.POSTGRES_9_4)
      block(sql)
    }
  }(databaseContext)

  def withTransaction[A](block: DSLContext => A): Future[A] = Future {
    db.withTransaction { connection =>
      val sql = DSL.using(connection, SQLDialect.POSTGRES_9_4)
      block(sql)
    }
  }(databaseContext)
}
