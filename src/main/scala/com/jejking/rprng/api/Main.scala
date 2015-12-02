package com.jejking.rprng.api

import java.security.SecureRandom

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.routing.RandomGroup
import akka.stream.ActorMaterializer
import com.jejking.rprng.rng.{RandomGeneratorFactory, RandomGeneratorSource, RandomSourceActor, SecureRandomSeeder}
import org.apache.commons.math3.random.ISAACRandom

/**
 * Starts web service.
 */
object Main {

  val randomRouterPath = "/user/randomRouter"

  def main(args: Array[String]): Unit = {

    implicit val actorSystem = ActorSystem("rprng")
    implicit val materializer = ActorMaterializer()

    val randomRouter = createActors(actorSystem)
    val streamsHelper = new AkkaStreamsHelper()
    val route = new Routes(streamsHelper).route

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

    // TODO - log shutdown
  }

  private def createActors(actorSystem: ActorSystem): ActorRef = {
    for (i <- 1 to 8) {
      val secureRandom = new SecureRandom()
      val secureSeeder = new SecureRandomSeeder(secureRandom)
      val randomGenerator = RandomGeneratorFactory.createNewGeneratorInstance[ISAACRandom]
      val randomGeneratorByteSource = RandomGeneratorSource(randomGenerator)
      actorSystem.actorOf(RandomSourceActor.props(randomGeneratorByteSource, secureSeeder), "randomByteSource" + i)
    }

    val paths = for (i <- 1 to 8) yield "/user/randomByteSource" + i

    actorSystem.actorOf(RandomGroup(paths).props(), "randomRouter")
  }

}
