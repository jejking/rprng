package com.jejking.rprng.rng


import akka.pattern.ask
import akka.actor.ActorSelection
import akka.stream.{Attributes, Outlet, SourceShape}
import akka.stream.stage.{GraphStage, GraphStageLogic, OutHandler}
import akka.util.{ByteString, Timeout}

import scala.concurrent.duration._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

/**
  * Created by jking on 24/05/2017.
  *
  * @param rngActorSelection reference to an actor or router which can reply to [[RandomByteRequest]] messages.
  */
class ByteStringSource(rngActorSelection: ActorSelection, byteStringSize: Int) extends GraphStage[SourceShape[ByteString]] {

  val out: Outlet[ByteString] = Outlet("NumbersSource")
  override val shape: SourceShape[ByteString] = SourceShape(out)
  implicit val timeout = Timeout(2 seconds)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = {
    new GraphStageLogic(shape) {

      val callback = getAsyncCallback[ByteString](byteString => push(out, byteString))

      setHandler(out, new OutHandler {
        override def onPull(): Unit = {
          val futureByteSTring = (rngActorSelection ? RandomByteRequest(byteStringSize)).mapTo[ByteString]
          futureByteSTring.foreach(byteString => callback.invoke(byteString))
        }
      })
    }
  }
}
