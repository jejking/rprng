package com.jejking.rprng.rng

import akka.actor.ActorLogging
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.{Cancel, Request}
import akka.util.Timeout
import com.jejking.rprng.rng.actors.Protocol.EightByteStringRequest
import com.jejking.rprng.rng.actors.RngActor

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Wraps a path to [[RngActor]] (or a router over a bunch of them) to  act as a stream publisher of
  * [[akka.util.ByteString.ByteStrings]] which are obtained asynchronously from the actor / router.
  *
  * In a web application such publishers are expected to be created at the API level and to be relatively short-lived.
  *
  * @param eightByteStringServicePath the wrapped actor path. Must exist and be able to process
  *                                   [[com.jejking.rprng.rng.actors.Protocol.EightByteStringRequest]] messages.
  */
class EightByteStringActorPublisher(val eightByteStringServicePath: String) extends ActorPublisher[EightByteString]
  with ActorLogging{

  val wrappedActorPath = context.actorSelection(eightByteStringServicePath)

  implicit val timeout = Timeout(5 seconds)

  override def receive: Receive = {

    // reply from wrapped actor, we pass this on to our subscriber
    case ebs:EightByteString => checkedOnNext(ebs)

    // bad error bubbling back from the wrapped actor / router, notify subscriber
    case akka.actor.Status.Failure(e) => checkedOnError(e)

    // subscriber wants a bunch of randomness, fire off corresponding requests to wrapped actor
    case Request(cnt) => sendEightByteStrings()

    // that's it, subscriber has had enough, clear ourselves up. This may lead to some messages in the dead letter queue
    // if they're still on the way from the wrapped actor when subscriber signalled it doesn't want them any more.
    case Cancel => {
      if (log.isDebugEnabled) {
        log.debug("shutting down publisher actor " + context.self.path)
      }

      context.stop(self)
    }

    // fallback default case - log and do nothing
    case m => log.warning(s"Got incomprehensible message $m")
  }

  def checkedOnNext(eightByteString: EightByteString): Unit = {
    // only if publisher still active and actual demand present
    if (isActive && totalDemand > 0) {
      onNext(eightByteString)
    }
  }

  def checkedOnError(t: Throwable): Unit = {
    log.error(t, "error message from wrapped actor " + this.eightByteStringServicePath + " in publisher " + context.self.path)
    if (!isErrorEmitted) {
      onError(t)
    }
  }

  def sendEightByteStrings() {
    for (i <- 1L to totalDemand) {
      wrappedActorPath ! EightByteStringRequest
    }
  }

}
