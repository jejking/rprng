package com.jejking.rprng.rng

import java.util.UUID
import java.util.concurrent.TimeUnit

import akka.actor._
import akka.actor.Actor.Receive
import org.apache.commons.math3.random.{RandomGenerator, MersenneTwister}

import RandomByteSourceRouterActor._

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._
import scala.reflect.ClassTag

/**
 * Actor that creates a configurable number of properly seeded [[RandomByteSourceActor]] instances. It then
 * does two main things with these:
 * - incoming requests are routed to a randomly selected child
 * - children are scheduled to be destroyed and recreated (with fresh seed) at random intervals
 */
class RandomByteSourceRouterActor[G <: RandomGenerator](val byteSourceFactory: RandomByteSourceFactory, val config: RandomByteSourceRouterActorConfig[G])(implicit val tag: ClassTag[G]) extends Actor {

  import scala.concurrent.ExecutionContext.Implicits.global
  val randomGenerator: G = tag.runtimeClass.newInstance().asInstanceOf[G]

  override def preStart(): Unit = {
    for (i <- 1 to config.numberChildren) {
      createChild()
    }
  }


  override def receive: Receive = {
    case NewChild => createChild()
    case rbr: RandomByteRequest => context.children.head.forward(rbr)
    case _ => println("got a message")
  }

  def createChild(): ActorRef = {

    val actorRef = createChildRandomByteSourceActor(context, byteSourceFactory)
    context.system.scheduler.scheduleOnce(computeScheduledTimeOfDeath(config, randomGenerator)) {
      context.system.stop(actorRef)
      self ! NewChild
    }
    actorRef
  }

}

object RandomByteSourceRouterActor {

  val defaultMinLifeTime = FiniteDuration(1, TimeUnit.HOURS)
  val defaultMaxLifeTime = FiniteDuration(8, TimeUnit.HOURS)

  case object NewChild

  case class RandomByteSourceRouterActorConfig[G <: RandomGenerator](numberChildren: Int, minLifeTime: FiniteDuration = defaultMinLifeTime, maxLifeTime: FiniteDuration = defaultMaxLifeTime)(implicit tag: ClassTag[G]) {
    require(numberChildren > 0, "Number of children must be greater than zero")
    require(minLifeTime < maxLifeTime, "minLifeTime must be less than maxLifeTime")
  }


  def props[G <: RandomGenerator](randomByteSourceFactory: RandomByteSourceFactory, config: RandomByteSourceRouterActorConfig[G])(implicit tag: ClassTag[G]): Props =
    Props(new RandomByteSourceRouterActor[G](randomByteSourceFactory, config))

  def createChildRandomByteSourceActor[G <: RandomGenerator](actorRefFactory: ActorRefFactory, byteSourceFactory: RandomByteSourceFactory)(implicit tag: ClassTag[G], ec: ExecutionContext): ActorRef = {
    val props = RandomByteSourceActor.props(byteSourceFactory.createRandomByteSource[G])
    actorRefFactory.actorOf(props, UUID.randomUUID().toString)
  }

  def computeScheduledTimeOfDeath(config: RandomByteSourceRouterActorConfig[_], randomGenerator: RandomGenerator): FiniteDuration = {
    // random duration at least min, at most max
    val actualDuration = config.maxLifeTime - config.minLifeTime
    val numberOfMillis = actualDuration.toMillis.asInstanceOf[Int]
    val randomInterval = randomGenerator.nextInt(numberOfMillis)
    config.minLifeTime + (randomInterval milliseconds)
  }




}
