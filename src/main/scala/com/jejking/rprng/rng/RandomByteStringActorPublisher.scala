package com.jejking.rprng.rng

import akka.actor.Props
import akka.pattern.ask
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.{Cancel, Request}
import akka.util.{Timeout, ByteString}

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.duration._

/**
 * Wraps a path to [[RandomByteSourceActor]] (or a router over a bunch of them) to
 * act as a stream publisher of `ByteString`s.
 */
class RandomByteStringActorPublisher(val byteStringSize: Int, val randomByteServicePath: String) extends ActorPublisher[ByteString]  {

  import scala.concurrent.ExecutionContext.Implicits.global

  val wrappedActorPath = context.actorSelection(randomByteServicePath)
  implicit val timeout = Timeout(5 seconds)


  override def receive: Receive = {
    case bs:ByteString => checkedOnNext(bs)
    case akka.actor.Status.Failure(e) => checkedOnError(e)
    case Request(cnt) => sendByteStrings()
    case Cancel => context.stop(self)
    case _ =>
  }

  def checkedOnNext(byteString: ByteString): Unit = {
    if (isActive && totalDemand > 0) {
      onNext(byteString)
    }
  }

  def checkedOnError(t: Throwable): Unit = {
    if (!isErrorEmitted) {
      onError(t)
    }
  }

  def sendByteStrings() {
    val capturedDemand = totalDemand.toInt
    Future {
      for (i <- 1 to capturedDemand) {
        wrappedActorPath ! RandomByteRequest(byteStringSize)
      }
    }
  }

}

object RandomByteStringActorPublisher {

  val standardActorPath = "/user/randomByteService"



  def props(byteStringSize: Int = 8, randomByteServicePath: String = standardActorPath): Props = Props(new RandomByteStringActorPublisher(byteStringSize, randomByteServicePath))
  
}