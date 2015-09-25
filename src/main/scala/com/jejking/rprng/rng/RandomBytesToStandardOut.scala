package com.jejking.rprng.rng

import java.security.SecureRandom

import akka.actor.ActorSystem
import akka.routing.RandomGroup
import akka.stream.ActorMaterializer
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.Source
import akka.util.ByteString
import org.apache.commons.math3.random.Well44497b
import org.reactivestreams.Publisher

/**
 * Creates a bunch of [[RandomByteSourceActor]] instances, puts them behind
 * an Akka Random Router, hooks up a reactive stream to the router and writes
 * the stream as raw bytes to stdout until the program is killed.
 *
 * Should be useful for getting an initial impression of the randomness quality by
 * hooking up the output to dieharder and co.
 */
object RandomBytesToStandardOut {

  def main(args: Array[String]): Unit = {

    implicit val actorSystem = ActorSystem("RandomByteToStandardOut")
    implicit val materializer = ActorMaterializer()

    for (i <- 1 to 8) {
      val secureRandom = new SecureRandom()
      val secureSeeder = new SecureRandomSeeder(secureRandom)
      val randomGenerator = RandomGeneratorFactory.createNewGeneratorInstance[Well44497b]
      val randomGeneratorByteSource = RandomGeneratorByteSource(randomGenerator)
      actorSystem.actorOf(RandomByteSourceActor.props(randomGeneratorByteSource, secureSeeder), "randomByteSource" + i)
    }

    val paths = for (i <- 1 to 8) yield "/user/randomByteSource" + i

    val routerActorRef = actorSystem.actorOf(RandomGroup(paths).props(), "randomRouter")

    val publisherActor = actorSystem.actorOf(RandomByteStringActorPublisher.props(8, "/user/randomRouter"))
    val streamActorPublisher: Publisher[ByteString] = ActorPublisher[ByteString](publisherActor)
    val source: Source[ByteString, Unit] = Source(streamActorPublisher)
    // System.out.write(bs.toArray)
    val runnableGraph = source.takeWhile(_ => true).runForeach(bs => System.out.write(bs.toArray))
    // runnableGraph.run()


  }

}
