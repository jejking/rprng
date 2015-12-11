package com.jejking.rprng.api

import java.security.SecureRandom
import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.routing.RandomGroup
import akka.stream.ActorMaterializer
import com.jejking.rprng.rng.RandomSourceActor.TimeRangeToReseed
import com.jejking.rprng.rng.{RandomGeneratorFactory, RandomGeneratorSource, RandomSourceActor, SecureRandomSeeder}
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.commons.math3.random.ISAACRandom

import scala.concurrent.duration._
/**
 * Starts web service.
 */
object Main {

  val randomRouterPath = "/user/randomRouter"

  def main(args: Array[String]): Unit = {

    val conf = ConfigFactory.load()
    val port = conf.getInt("rprng.port")
    require(port > 0, s"Port must be greater than zero, but is $port")


    implicit val actorSystem = ActorSystem("rprng")
    implicit val materializer = ActorMaterializer()

    val randomRouter = createActors(actorSystem, conf)
    val streamsHelper = new AkkaStreamsHelper()
    val route = new Routes(streamsHelper).route

    val bindingFuture = Http().bindAndHandle(route, "localhost", port)

    // TODO - log shutdown
  }

  private def getTimeRangeToReseed(conf: Config) = {
    val min = conf.getDuration("rprng.reseedMin", TimeUnit.MILLISECONDS)
    val max = conf.getDuration("rprng.reseedMax", TimeUnit.MILLISECONDS)

    TimeRangeToReseed(min milliseconds, max milliseconds)
  }

  private def createActors(actorSystem: ActorSystem, conf: Config): ActorRef = {
    val actorCount = conf.getInt("rprng.actorCount")
    require(actorCount > 0, s"Actor count must be greater than zero, but is $actorCount")

    val timeRangeToReseed = getTimeRangeToReseed(conf)

    for (i <- 1 to actorCount) {
      val secureRandom = new SecureRandom()
      val secureSeeder = new SecureRandomSeeder(secureRandom)
      val randomGenerator = RandomGeneratorFactory.createNewGeneratorInstance[ISAACRandom]
      val randomGeneratorByteSource = RandomGeneratorSource(randomGenerator)
      actorSystem.actorOf(RandomSourceActor.props(randomGeneratorByteSource, secureSeeder, timeRangeToReseed), "randomByteSource" + i)
    }

    val paths = for (i <- 1 to 8) yield "/user/randomByteSource" + i

    actorSystem.actorOf(RandomGroup(paths).props(), "randomRouter")
  }

}
