package com.jejking.rprng.rng

import akka.actor.{ActorLogging, Props}
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.{Cancel, Request}
import akka.util.{ByteString, Timeout}
import com.jejking.rprng.rng.actors.RngActor

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * Wraps a path to [[RngActor]] (or a router over a bunch of them) to
 * act as a stream publisher of `ByteString`s which are obtained asynchronously from the actor / router. When wrapping
 * a router the publisher thus effectively mixes up the underlying sources of pseudo-randomness.
 *
 * In a web application such publishers are expected to be created at the API level and to be relatively short-lived.
 *
 * @param byteStringSize the number of bytes to be requested
 * @param randomByteServicePath the wrapped actor path. Must exist and be able to process [[RandomByteRequest]] messages.
 */
class RandomByteStringActorPublisher(val byteStringSize: Int, val randomByteServicePath: String)
      extends ActorPublisher[ByteString] with ActorLogging {

  val wrappedActorPath = context.actorSelection(randomByteServicePath)

  implicit val timeout = Timeout(5 seconds)

  override def receive: Receive = {
    // reply from wrapped actor, we pass this on to our subscriber
    case bs:ByteString => checkedOnNext(bs)

    // bad error bubbling back from the wrapped actor / router, notify subscriber
    case akka.actor.Status.Failure(e) => checkedOnError(e)

    // subscriber wants a bunch of randomness, fire off corresponding requests to wrapped actor
    case Request(cnt) => sendByteStrings()

    // that's it, subscriber has had enough, clear ourselves up. This may lead to some messages in the dead letter queue
    // if they're still on the way from the wrapped actor when subscriber signalled it doesn't want them any more.
    case Cancel => {
      if (log.isDebugEnabled) {
        log.debug("shutting down publisher actor " + context.self.path)
      }

      context.stop(self)
    }
    case _ =>
  }

  def checkedOnNext(byteString: ByteString): Unit = {
    // only if publisher still active and actual demand present
    if (isActive && totalDemand > 0) {
      onNext(byteString)
    }
  }

  def checkedOnError(t: Throwable): Unit = {
    log.error(t, "error message from wrapped actor " + this.randomByteServicePath + " in publisher " + context.self.path)
    if (!isErrorEmitted) {
      onError(t)
    }
  }

  def sendByteStrings() {
    // capture current demand and fire off that number of requests to the underlying actor without blocking
    // The value is captured to allow for mutation of the variable by incoming demand and we fire off
    // the corresponding number of requests to the random router.

    // Note that the RandomSourceActors will send their replies back here in the form of ByteStrings.
    val capturedDemand = totalDemand.toInt

    for (i <- 1 to capturedDemand) {
      wrappedActorPath ! RandomByteRequest(byteStringSize)
    }
  }

}

object RandomByteStringActorPublisher {

  val standardActorPath = "/user/randomRouter"

  def props(byteStringSize: Int = 8, randomByteServicePath: String = standardActorPath): Props = Props(new RandomByteStringActorPublisher(byteStringSize, randomByteServicePath))
  
}