package akka.contrib.persistence.mongodb

import akka.persistence.{AtomicWrite, PersistentRepr}

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import scala.collection.immutable.{Seq => ISeq}

class ScalaDriverJournaller(driver: ScalaMongoDriver) extends MongoPersistenceJournallingApi {


  private[this] def doBatchAppend(writes: ISeq[AtomicWrite], collection: Future[BSONCollection])(implicit ec: ExecutionContext): Future[ISeq[Try[Unit]]] = {
    val batch = writes.map(aw => Try(driver.serializeJournal(Atom[BSONDocument](aw, driver.useLegacySerialization))))

    if (batch.forall(_.isSuccess)) {
      val collected = batch.toStream.collect { case Success(doc) => doc }
      collection.flatMap(_.bulkInsert(collected, ordered = true, writeConcern).map(_ => batch.map(_.map(_ => ()))))
    } else {
      Future.sequence(batch.map {
        case Success(document: BSONDocument) =>
          collection.flatMap(_.insert(document, writeConcern).map(writeResultToUnit))
        case f: Failure[_] => Future.successful(Failure[Unit](f.exception))
      })
    }
  }

  override private[mongodb] def batchAppend(writes: immutable.Seq[AtomicWrite])(implicit ec: ExecutionContext): Future[ISeq[Try[Unit]]] = {


    ???
  }


  override private[mongodb] def deleteFrom(persistenceId: String, toSequenceNr: Long)(implicit ec: ExecutionContext) = ???

  override private[mongodb] def replayJournal(pid: String, from: Long, to: Long, max: Long)(replayCallback: PersistentRepr => Unit)(implicit ec: ExecutionContext) = ???

  override private[mongodb] def maxSequenceNr(pid: String, from: Long)(implicit ec: ExecutionContext) = ???
}
