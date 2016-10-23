package com.jejking.rprng.main

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.jejking.rprng.rng.RandomSourceActor.TimeRangeToReseed
import com.jejking.rprng.rng._
import org.reactivestreams.Publisher

import scala.concurrent.duration._

import scala.language.postfixOps

/**
 * Creates a bunch of [[RandomSourceActor]] instances, puts them behind
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

    val routerActorRef = createRandomSourceActors(actorSystem, RprngConfig(0, TimeRangeToReseed(1 hour, 8 hours), 8))
    val publisherActor = actorSystem.actorOf(RandomByteStringActorPublisher.props(512, routerActorRef.path.toString))

    val streamActorPublisher: Publisher[ByteString] = ActorPublisher[ByteString](publisherActor)
    Source.fromPublisher(streamActorPublisher)
          .takeWhile(_ => true)
          .runForeach(bs => System.out.write(bs.toArray))


  }

}
