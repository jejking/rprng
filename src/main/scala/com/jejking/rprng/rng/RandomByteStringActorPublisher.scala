package com.jejking.rprng.rng

import akka.actor.Props
import akka.pattern.ask
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.{Cancel, Request}
import akka.util.ByteString

import scala.util.{Failure, Success}

/**
 * Wraps a path to [[RandomByteSourceActor]] (or a router over a bunch of them) to
 * act as a stream publisher of `ByteString`s.
 */
class RandomByteStringActorPublisher(val byteStringSize: Int, val randomByteServicePath: String) extends ActorPublisher[ByteString]  {

  val wrappedActorPath = context.actorSelection(randomByteServicePath)


  override def receive: Receive = {
    case Request(cnt) => sendByteStrings()
    case Cancel => context.stop(self)
    case _ =>
  }

  def sendByteStrings() {
    while(isActive && totalDemand > 0) {
      (wrappedActorPath ? RandomByteRequest(byteStringSize)).mapTo[ByteString].onComplete {
        case Success(bs) => onNext(bs)
        case Failure(e) => onError(e)
      }
    }
  }

}

object RandomByteStringActorPublisher {

  val standardActorPath = "/user/randomByteService"

  def props(byteStringSize: Int = 8, randomByteServicePath: String = standardActorPath): Props = Props(new RandomByteStringActorPublisher(byteStringSize, randomByteServicePath))
  
}