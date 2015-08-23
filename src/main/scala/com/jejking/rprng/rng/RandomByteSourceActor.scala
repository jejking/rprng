package com.jejking.rprng.rng

import akka.actor.Actor
import akka.actor.Actor.Receive
import akka.util.ByteString
import com.jejking.rprng.rng.RandomByteSourceActor.{UnknownInputType, RandomByteRequest}

/**
 * Created by jking on 23/08/15.
 */
class RandomByteSourceActor(private val randomByteSource: RandomByteSource) extends Actor {

  override def receive: Receive = {
    case RandomByteRequest(count) => sender() ! ByteString(randomByteSource.randomBytes(count))
    case _ => sender() ! UnknownInputType
  }
}

object RandomByteSourceActor {

  case class RandomByteRequest(count: Int)

  sealed trait Error
  case object UnknownInputType extends Error

}