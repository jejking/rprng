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
 * act as a stream publisher of `Ints`s which are obtained asynchronously from the actor / router. When wrapping
 * a router the publisher thus effectively mixes up the underlying sources of pseudo-randomness.
 *
 * In a web application such publishers are expected to be created at the API level and to be relatively short-lived.
 *
 * @param randomIntRequest prototypical random int request, used to generate the resulting stream
 * @param randomServicePath the wrapped actor path. Must exist and be able to process [[RandomIntRequest]] messages.
 */
class RandomIntActorPublisher(val randomIntRequest: RandomIntRequest, val randomServicePath: String)
  extends ActorPublisher[Int] with ActorLogging {

  val wrappedActorPath = context.actorSelection(randomServicePath)
  implicit val timeout = Timeout(5 seconds)


  override def receive: Receive = {
    // reply from wrapped actor, we pass this on to our subscriber
    case i:Int => checkedOnNext(i)

    // bad error bubbling back from the wrapped actor / router, notify subscriber
    case akka.actor.Status.Failure(e) => checkedOnError(e)

    // subscriber wants a bunch of ints, fire off corresponding requests to wrapped actor
    case Request(cnt) => sendInts()

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

  def checkedOnNext(i: Int): Unit = {
    // only if publisher still active and actual demand present
    if (isActive && totalDemand > 0) {
      onNext(i)
    }
  }

  def checkedOnError(t: Throwable): Unit = {
    log.error(t, "error message from wrapped actor " + this.randomServicePath + " in publisher " + context.self.path)
    if (!isErrorEmitted) {
      onError(t)
    }
  }

  def sendInts() {
    val capturedDemand = totalDemand.toInt
    for (i <- 1 to capturedDemand) {
      wrappedActorPath ! this.randomIntRequest
    }
  }

}

object RandomIntActorPublisher {

  val standardActorPath = "/user/randomRouter"

  def props(randomIntRequest: RandomIntRequest, randomServicePath: String = standardActorPath): Props = Props(new RandomIntActorPublisher(randomIntRequest, randomServicePath))

}

