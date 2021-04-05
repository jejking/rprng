package com.jejking.rprng.main

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.stream.{ActorMaterializer, Graph, SourceShape}
import akka.util.ByteString
import com.jejking.rprng.rng._
import com.jejking.rprng.rng.actors.TimeRangeToReseed

import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * Creates a bunch of [[com.jejking.rprng.rng.actors.RngActor]] instances, puts them behind
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

    val sourceGraph: Graph[SourceShape[ByteString], NotUsed] = new ByteStringSource(actorSystem.actorSelection(routerActorRef.path), 512)

    Source.fromGraph(sourceGraph)
      .takeWhile(_ => true)
      .runForeach(bs => System.out.write(bs.toArray))

  }

}
