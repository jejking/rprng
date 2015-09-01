package com.jejking.rprng.rng

import akka.actor.{Props, Actor}
import akka.actor.Actor.Receive
import akka.util.ByteString
import com.jejking.rprng.rng.RandomByteSourceActor.{GeneratorClass, UnknownInputType}

/**
 * Actor wrapping a [[RandomByteSource]] to allow thread-safe access to it.
 */
class RandomByteSourceActor(private val randomByteSource: RandomByteSource) extends Actor {

  override def receive: Receive = {
    case RandomByteRequest(count) => sender() ! ByteString(randomByteSource.randomBytes(count))
    case GeneratorClass => sender() ! randomByteSource.generatorClass()
    case _ => sender() ! UnknownInputType
  }
}

object RandomByteSourceActor {


  case object GeneratorClass

  sealed trait Error
  case object UnknownInputType extends Error

  def props(randomByteSource: RandomByteSource): Props = Props(new RandomByteSourceActor(randomByteSource))

}