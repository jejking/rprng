package com.jejking.rprng.rng

import java.util.concurrent.TimeUnit

import akka.actor._
import akka.actor.Actor.Receive
import akka.util.ByteString
import com.jejking.rprng.rng.RandomByteSourceActor.{TimeRangeToReseed, UnknownInputType}
import org.apache.commons.math3.random.RandomGenerator

import scala.concurrent.{Future, ExecutionContext}
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
                            private val scheduleHelperCreator: (ActorSystem) => ScheduleHelper = a => new AkkaScheduleHelper(a.scheduler),
                            private val timeRangeToReseed: TimeRangeToReseed = TimeRangeToReseed()) extends Actor {

  import RandomByteSourceActor._
  import scala.concurrent.ExecutionContext.Implicits.global

  val scheduleHelper  =  scheduleHelperCreator(context.system)

  override def preStart(): Unit = {
    val seed = secureSeeder.generateSeed()
    this.byteSource.reseed(seed)
    scheduleReseed()
  }

  override def receive: Receive = {
    case r: RandomByteRequest => sender() ! ByteString(byteSource.randomBytes(r))
    case Reseed => fetchSeedAndNotify()
    case newSeed: NewSeed => applyNewSeedAndScheduleReseed(newSeed)
    case _ => sender() ! UnknownInputType
  }

  def applyNewSeedAndScheduleReseed(newSeed: NewSeed): Unit = {
    this.byteSource.reseed(newSeed.seed)
    scheduleReseed()
  }

  def fetchSeedAndNotify(): Unit = {
    Future {
      val seed = secureSeeder.generateSeed()
      self ! NewSeed(seed)
    }
  }

  def scheduleReseed(): Unit = {
    val timeToReseed = computeScheduledTimeToReseed(timeRangeToReseed, byteSource)
    scheduleHelper.scheduleOnce(timeToReseed) {
      self ! Reseed
    }
  }
}

// needed to allow straightforward mocking...
/**
 * Isolates the functionality we need from the Akka Scheduler.
 */
trait ScheduleHelper {
  def scheduleOnce(delay: FiniteDuration)(f: ⇒ Unit)(implicit executor: ExecutionContext): Cancellable
}

/**
 * Standard implementation that takes the akka scheduler.
 * @param scheduler
 */
class AkkaScheduleHelper(scheduler: Scheduler) extends ScheduleHelper {

  override def scheduleOnce(delay: FiniteDuration)(f: ⇒ Unit)(implicit executor: ExecutionContext): Cancellable =  {
    scheduler.scheduleOnce(delay)(f)
  }
}


object RandomByteSourceActor {

  val defaultMinLifeTime = FiniteDuration(1, TimeUnit.HOURS)
  val defaultMaxLifeTime = FiniteDuration(8, TimeUnit.HOURS)

  case class TimeRangeToReseed(minLifeTime: FiniteDuration = defaultMinLifeTime, maxLifeTime: FiniteDuration = defaultMaxLifeTime) {
    require(minLifeTime < maxLifeTime, "minLifeTime must be less than maxLifeTime")
  }

  case class NewSeed(seed: Long) extends AnyVal

  case object Reseed
  sealed trait Error
  case object UnknownInputType extends Error

  def props(byteSource: RandomByteSource, secureSeeder: SecureSeeder): Props = Props(new RandomByteSourceActor(byteSource, secureSeeder))

  def computeScheduledTimeToReseed(config: TimeRangeToReseed, byteSource: RandomByteSource): FiniteDuration = {
    // random duration at least min, at most max
    val actualDuration = config.maxLifeTime - config.minLifeTime
    val numberOfMillis = actualDuration.toMillis.asInstanceOf[Int]
    val randomInterval = byteSource.nextInt(numberOfMillis)
    println("number of millis: " + numberOfMillis, "random interval: " + randomInterval)
    config.minLifeTime + (randomInterval milliseconds)
  }



}