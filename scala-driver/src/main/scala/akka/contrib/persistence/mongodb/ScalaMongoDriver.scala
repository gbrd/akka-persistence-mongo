package akka.contrib.persistence.mongodb

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.mongodb.scaladsl.MongoSource
import akka.stream.scaladsl.Sink
import com.mongodb.ConnectionString
import com.typesafe.config.Config
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.bson.codecs.configuration.CodecRegistry
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.model.{CreateCollectionOptions, IndexOptions, Indexes}
import org.mongodb.scala.{MongoClient, MongoCollection}

import scala.concurrent.{ExecutionContext, Future}

object ScalaMongoDriver {

  case class CollectionOptions(capped: Boolean)

  case class CollectionDetails(name: String, `type`: String, options: CollectionOptions)

  val codecRegistry: CodecRegistry = fromRegistries(fromProviders(
    classOf[CollectionOptions],
    classOf[CollectionDetails]
  ), DEFAULT_CODEC_REGISTRY)
}

class ScalaMongoDriver(system: ActorSystem, config: Config) extends MongoPersistenceDriver(system, config) {

  import ScalaMongoDriver._

  override type C = Future[MongoCollection[Document]]
  override type D = Document


  implicit val mat = ActorMaterializer()

  private[this] lazy val connectionString = new ConnectionString(mongoUri)

  private[mongodb] lazy val client = MongoClient(mongoUri)


  private val db = client.getDatabase(databaseName.getOrElse(Option(connectionString.getDatabase).getOrElse(DEFAULT_DB_NAME)))
    .withCodecRegistry(codecRegistry)

  override private[mongodb] def collection(name: String): Future[MongoCollection[Document]] = Future.successful(db.getCollection(name))

  override private[mongodb] def cappedCollection(name: String)(implicit ec: ExecutionContext): Future[MongoCollection[Document]] = {

    def createIt: Future[MongoCollection[Document]] = {
      val options = CreateCollectionOptions().capped(true).sizeInBytes(realtimeCollectionSize)
      db.createCollection(name, options).toFuture().map(_ => db.getCollection(name))
    }

    MongoSource(db.listCollections[CollectionDetails]()).filter(_.name == name).runWith(Sink.headOption).flatMap {
      case Some(details) if details.options.capped => Future.successful(db.getCollection(name))
      case Some(details) if !details.options.capped =>
        db.getCollection(name).drop().toFuture().flatMap(_ => createIt)
      case None => createIt
    }
  }

  override private[mongodb] def ensureIndex(indexName: String, unique: Boolean, sparse: Boolean, fields: (String, Int)*)(implicit ec: ExecutionContext): C => C = { collection =>
    val ky = Indexes.compoundIndex(fields.toSeq.map { case (f, o) => (if (o > 0) Indexes.ascending(f) else Indexes.descending(f)) }: _*)
    collection.map(_.createIndex(ky,IndexOptions().sparse(sparse).unique(unique).name(indexName)))
    collection
  }

  override private[mongodb] def closeConnections(): Unit = ???

  override private[mongodb] def upgradeJournalIfNeeded(): Unit = ???

  override private[mongodb] def upgradeJournalIfNeeded(persistenceId: String): Unit = ???


}
