package com.jejking.rprng.rng

import java.util.concurrent.TimeUnit

import akka.actor.{Props, Actor}
import akka.actor.Actor.Receive
import akka.util.ByteString
import com.jejking.rprng.rng.RandomByteSourceActor.{GeneratorClass, UnknownInputType}
import org.apache.commons.math3.random.RandomGenerator

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._

/**
 * Actor wrapping a [[RandomByteSource]] to allow thread-safe access to it and to manage its lifecycle,
 * especially with regard to re-seeding.
 */
class RandomByteSourceActor(private val byteSource: RandomByteSource) extends Actor {

  override def receive: Receive = {
    case r: RandomByteRequest => sender() ! ByteString(byteSource.randomBytes(r))
    case GeneratorClass => sender() ! byteSource.generatorClass()
    case _ => sender() ! UnknownInputType
  }
}

object RandomByteSourceActor {

  val defaultMinLifeTime = FiniteDuration(1, TimeUnit.HOURS)
  val defaultMaxLifeTime = FiniteDuration(8, TimeUnit.HOURS)

  case class LifeSpanRange(minLifeTime: FiniteDuration = defaultMinLifeTime, maxLifeTime: FiniteDuration = defaultMaxLifeTime) {
    require(minLifeTime < maxLifeTime, "minLifeTime must be less than maxLifeTime")
  }

  case object GeneratorClass

  sealed trait Error
  case object UnknownInputType extends Error

  def props(byteSource: RandomByteSource): Props = Props(new RandomByteSourceActor(byteSource))

  def computeScheduledTimeOfDeath(config: LifeSpanRange, randomGenerator: RandomGenerator): FiniteDuration = {
    // random duration at least min, at most max
    val actualDuration = config.maxLifeTime - config.minLifeTime
    val numberOfMillis = actualDuration.toMillis.asInstanceOf[Int]
    val randomInterval = randomGenerator.nextInt(numberOfMillis)
    config.minLifeTime + (randomInterval milliseconds)
  }

}