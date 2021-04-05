package com.jejking.rprng.rng

import akka.actor.ActorSelection
import akka.pattern.ask
import akka.stream.stage.{GraphStage, GraphStageLogic, OutHandler}
import akka.stream.{Attributes, Outlet, SourceShape}
import akka.util.{ByteString, Timeout}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Exposes the [[com.jejking.rprng.rng.actors.RngActor]] instances (normally behind an additional
  * [[akka.routing.RandomGroup]]) as an Akka Streams [[SourceShape]] for use in type-safe
  * stream processing.
  *
  * It is expected that each incoming request for randomness will create - and then discard - a fresh
  * source and configure it to provide appropriately-sized [[ByteString]]s.
  *
  * @param rngActorSelection reference to an actor or router which can reply to [[RandomByteRequest]] messages
  *                          with [[ByteString]] instances of the requested size.
  * @param byteStringSize determines the size of the [[ByteString]]s to be emitted by the stream.
  *                       Must be a strictly positive integer.
  */
class ByteStringSource(rngActorSelection: ActorSelection, val byteStringSize: Int) extends GraphStage[SourceShape[ByteString]] {

  require(byteStringSize > 0, s"byteStringSize must be greater than 0, but is ${byteStringSize}")

  val out: Outlet[ByteString] = Outlet("ByteStringSource")
  override val shape: SourceShape[ByteString] = SourceShape(out)
  implicit val timeout = Timeout(2 seconds)


  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = {
    new GraphStageLogic(shape) {

      val callback = getAsyncCallback[ByteString](byteString => push(out, byteString))

      setHandler(out, new OutHandler {
        override def onPull(): Unit = {
          val futureByteString = (rngActorSelection ? RandomByteRequest(byteStringSize)).mapTo[ByteString]
          futureByteString.foreach(byteString => callback.invoke(byteString))
        }
      })
    }
  }
}
