package com.jejking.rprng.rng

import java.util.concurrent.TimeUnit

import akka.actor._
import akka.actor.Actor.Receive
import akka.util.ByteString
import com.jejking.rprng.rng.RandomByteSourceActor.{GeneratorClass, UnknownInputType}
import org.apache.commons.math3.random.RandomGenerator

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._

/**
 * Actor wrapping a [[RandomGeneratorByteSource]] to allow thread-safe access to it and to manage its lifecycle,
 * especially with regard to re-seeding.
 *
 * @param byteSource PRNG wrapped by actor
 * @param secureSeeder source for higher-quality seed for the PRNG
 */
class RandomByteSourceActor(private val byteSource: RandomByteSource, private val secureSeeder: SecureSeeder,
                            private val scheduleHelperCreator: (ActorSystem) => ScheduleHelper = a => new AkkaScheduleHelper(a.scheduler)) extends Actor {

  val scheduleHelper  =  scheduleHelperCreator(context.system)

  override def preStart(): Unit = {
    val seed = secureSeeder.generateSeed()
    this.byteSource.reseed(seed)
  }

  override def receive: Receive = {
    case r: RandomByteRequest => sender() ! ByteString(byteSource.randomBytes(r))
    case _ => sender() ! UnknownInputType
  }


}

// needed to allow straightforward mocking...
/**
 * Isolates the functionality we need from the Akka Scheduler.
 */
trait ScheduleHelper {
  def scheduleOnce(delay: FiniteDuration)(f: ⇒ Unit)(implicit executor: ExecutionContext): Cancellable
}

class AkkaScheduleHelper(scheduler: Scheduler) extends ScheduleHelper {

  override def scheduleOnce(delay: FiniteDuration)(f: ⇒ Unit)(implicit executor: ExecutionContext): Cancellable =  {
    scheduler.scheduleOnce(delay)(f)
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

  def props(byteSource: RandomGeneratorByteSource, secureSeeder: SecureRandomSeeder): Props = Props(new RandomByteSourceActor(byteSource, secureSeeder))

  def computeScheduledTimeOfDeath(config: LifeSpanRange, randomGenerator: RandomGenerator): FiniteDuration = {
    // random duration at least min, at most max
    val actualDuration = config.maxLifeTime - config.minLifeTime
    val numberOfMillis = actualDuration.toMillis.asInstanceOf[Int]
    val randomInterval = randomGenerator.nextInt(numberOfMillis)
    config.minLifeTime + (randomInterval milliseconds)
  }

}