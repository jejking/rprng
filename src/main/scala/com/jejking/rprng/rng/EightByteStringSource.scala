package com.jejking.rprng.rng


import akka.pattern.ask
import akka.actor.ActorSelection
import akka.stream.{Attributes, Outlet, SourceShape}
import akka.stream.stage.{GraphStage, GraphStageLogic, OutHandler}
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import com.jejking.rprng.rng.actors.Protocol.EightByteStringRequest


/**
  * Created by jking on 24/05/2017.
  *
  * @param eightByteStringActor reference to an actor or router which can reply to [[com.jejking.rprng.rng.actors.Protocol.EightByteStringRequest]] messages.
  */
class EightByteStringSource(eightByteStringActor: ActorSelection) extends GraphStage[SourceShape[EightByteString]] {

  val out: Outlet[EightByteString] = Outlet("NumbersSource")
  override val shape: SourceShape[EightByteString] = SourceShape(out)
  implicit val timeout = Timeout(2 seconds)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = {
    new GraphStageLogic(shape) {

      val callback = getAsyncCallback[EightByteString](ebs => push(out, ebs))

      setHandler(out, new OutHandler {
        override def onPull(): Unit = {
          val futureEightByteString = (eightByteStringActor ? EightByteStringRequest).mapTo[EightByteString]
          futureEightByteString.foreach(ebs => callback.invoke(ebs))
        }
      })
    }
  }
}
