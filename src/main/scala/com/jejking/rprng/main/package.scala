package com.jejking.rprng

import java.security.SecureRandom
import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem}
import akka.routing.RandomGroup
import com.jejking.rprng.rng.actors.{RngActor, TimeRangeToReseed}
import com.jejking.rprng.rng._
import com.typesafe.config.Config
import org.apache.commons.math3.random.ISAACRandom

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Couple of useful types and functions for our main methods.
  */
package object main {

  case class RprngConfig(port: Int, timeRangeToReseed: TimeRangeToReseed, randomSourceActorCount: Int)

  def processConfig(conf: Config): RprngConfig = {
    val port = conf.getInt("rprng.port")
    require(port > 0, s"Port must be greater than zero, but is $port")

    val min = conf.getDuration("rprng.reseedMin", TimeUnit.MILLISECONDS)
    val max = conf.getDuration("rprng.reseedMax", TimeUnit.MILLISECONDS)

    val timeRangeToReseed = TimeRangeToReseed(min milliseconds, max milliseconds)

    val actorCount = conf.getInt("rprng.actorCount")
    require(actorCount > 0, s"Actor count must be greater than zero, but is $actorCount")

    RprngConfig(port, timeRangeToReseed, actorCount)
  }

  /**
    * Sets up the required number of [[RngActor]] instances behind
    * an akka [[RandomGroup]].
    *
    * @param actorSystem the actor system to use to set up the actors
    * @param myConfig the config
    * @return reference to the router in front of the newly created actors
    */
  def createRandomSourceActors(actorSystem: ActorSystem, myConfig: RprngConfig): ActorRef = {

    // generates required number of actors and returns their paths
    val paths = (1 to myConfig.randomSourceActorCount).map { i =>
      val secureRandom = new SecureRandom()
      val secureSeeder = new SecureRandomSeeder(secureRandom)
      val randomGenerator = RandomGeneratorFactory.createNewGeneratorInstance[ISAACRandom]
      val randomGeneratorByteSource = CommonsMathRng(randomGenerator)
      actorSystem.actorOf(RngActor.props(randomGeneratorByteSource, secureSeeder, myConfig.timeRangeToReseed), "randomByteSource" + i)
    }
      .map(_.path.toString)

    // constructs random router around the actor paths
    actorSystem.actorOf(RandomGroup(paths).props(), "randomRouter")
  }
}
