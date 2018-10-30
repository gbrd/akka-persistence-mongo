package akka.contrib.persistence.mongodb

import akka.actor.ActorSystem
import com.typesafe.config.Config

class ScalaDriverPersistenceExtension(actorSystem: ActorSystem) extends MongoPersistenceExtension {
  override def configured(config: Config): ConfiguredExtension = ???
}
