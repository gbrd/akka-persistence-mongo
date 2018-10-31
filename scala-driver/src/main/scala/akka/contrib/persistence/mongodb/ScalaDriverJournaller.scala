package akka.contrib.persistence.mongodb

import akka.actor.ActorSystem
import akka.persistence.{AtomicWrite, PersistentRepr}
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.collection.immutable.Document

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import scala.collection.immutable.{Seq => ISeq}

class ScalaDriverJournaller(driver: ScalaMongoDriver) extends MongoPersistenceJournallingApi {

  private implicit val system: ActorSystem = driver.actorSystem


  private[this] def doBatchAppend(writes: ISeq[AtomicWrite], collection: Future[MongoCollection[Document]])(implicit ec: ExecutionContext): Future[ISeq[Try[Unit]]] = {


    //val batch = writes.map(write => Try(driver.serializeJournal(Atom[Document](write, driver.useLegacySerialization))))
    ???

  }

  override private[mongodb] def batchAppend(writes: immutable.Seq[AtomicWrite])(implicit ec: ExecutionContext): Future[ISeq[Try[Unit]]] = {


    ???
  }


  override private[mongodb] def deleteFrom(persistenceId: String, toSequenceNr: Long)(implicit ec: ExecutionContext) = ???

  override private[mongodb] def replayJournal(pid: String, from: Long, to: Long, max: Long)(replayCallback: PersistentRepr => Unit)(implicit ec: ExecutionContext) = ???

  override private[mongodb] def maxSequenceNr(pid: String, from: Long)(implicit ec: ExecutionContext) = ???
}
