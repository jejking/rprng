package com.jejking.rprng.rng

import java.util.concurrent.TimeUnit

import akka.actor._
import akka.util.ByteString
import com.jejking.rprng.rng.RandomSourceActor.TimeRangeToReseed

import scala.concurrent.duration.{FiniteDuration, _}
import scala.concurrent.{ExecutionContext, Future}

import scala.language.postfixOps

/**
 * Actor wrapping a [[RandomSource]] to allow thread-safe access to it and to manage its lifecycle,
 * especially with regard to re-seeding.
 *
 * @param randomSource PRNG wrapped by actor
 * @param secureSeeder source for higher-quality seed for the PRNG
 */
class RandomSourceActor(private val randomSource: RandomSource, private val secureSeeder: SecureSeeder,
                            private val scheduleHelperCreator: (ActorSystem) => ScheduleHelper = a => new AkkaScheduleHelper(a.scheduler),
                            private val timeRangeToReseed: TimeRangeToReseed = TimeRangeToReseed()) extends Actor with ActorLogging {

  import RandomSourceActor.Protocol._
  import RandomSourceActor._

  import scala.concurrent.ExecutionContext.Implicits.global

  val scheduleHelper  =  scheduleHelperCreator(context.system)

  override def preStart(): Unit = {
    log.info(s"reseed interval is: $timeRangeToReseed")
    val seed = secureSeeder.generateSeed()
    this.randomSource.reseed(seed)
    scheduleReseed()
    log.info("completed pre-start of " + context.self.path)

  }

  override def receive: Receive = {

    // main use case, fetch random bytes
    case r: RandomByteRequest => {
      sender() ! ByteString(randomSource.randomBytes(r))
      if (log.isDebugEnabled) {
        log.debug("processed request for " + r.count + "bytes")
      }
    }

    case RandomAnyIntRequest => {
      sender() ! randomSource.nextInt()
      if (log.isDebugEnabled) {
        log.debug("processed request for random int with no specified bounds")
      }
    }

    case r: RandomIntRequest => {
      sender() ! randomSource.nextInt(r)
      if (log.isDebugEnabled) {
        val min = r.minBound
        val max = r.maxBound
        log.debug(s"processed request for random int between $min and $max")
      }
    }

    // trigger reseed, note this is done in a future to avoid blocking the actor
    case Reseed => fetchSeedAndNotify()

    // apply fresh seed, non blocking action
    case newSeed: NewSeed => applyNewSeedAndScheduleReseed(newSeed)

    // ooops
    case _ => {
      log.warning("received unknown message type")
      sender() ! UnknownInputType
    }
  }

  def applyNewSeedAndScheduleReseed(newSeed: NewSeed): Unit = {
    this.randomSource.reseed(newSeed.seed)
    log.info("applied new seed in actor " + context.self.path)

    // and off we go for the next round at some point in the future
    scheduleReseed()
  }

  def fetchSeedAndNotify(): Unit = {
    log.info("about to trigger future to collect fresh seed for actor " + context.self.path)
    // generate seed is likely to block as the seeder gathers entropy
    // therefore we do this in a future and send a message onto the actor's mailbox
    // for async application once we have a result
    Future {
      val seed = secureSeeder.generateSeed()
      log.info("obtained fresh seed in actor " + context.self.path)
      self ! NewSeed(seed)
    }
  }

  def scheduleReseed(): Unit = {
    // non-blocking computation
    val timeToReseed = computeScheduledTimeToReseed(timeRangeToReseed, randomSource)
    scheduleHelper.scheduleOnce(timeToReseed) {
      self ! Reseed
      log.info("sent reseed message to actor " + context.self.path)
    }
    log.info("scheduled reseed in actor " + context.self.path)
  }
}

/**
 * Isolates the functionality we need from the Akka Scheduler.
 */
trait ScheduleHelper {
  def scheduleOnce(delay: FiniteDuration)(f: ⇒ Unit)(implicit executor: ExecutionContext): Cancellable
}

/**
 * Standard implementation that simply delegates to the akka scheduler.
 * @param scheduler reference to an akka scheduler to delegate to
 */
class AkkaScheduleHelper(scheduler: Scheduler) extends ScheduleHelper {

  override def scheduleOnce(delay: FiniteDuration)(f: ⇒ Unit)(implicit executor: ExecutionContext): Cancellable =  {
    scheduler.scheduleOnce(delay)(f)
  }
}

/**
 * Defines constants and helper function.
 */
object RandomSourceActor {

  object Protocol {

    /**
     * Wraps long that encapsulates eight bytes of seed to apply to the underlying PRNG.
     * @param seed a long
     */
    case class NewSeed(seed: Long) extends AnyVal

    /**
     * Instruction to the actor to ask for more seed to apply
     */
    case object Reseed

    // errors
    sealed trait Error
    case object UnknownInputType extends Error
  }


  /**
   * Default time to reseed is between one and eight hours, a completely arbitrary time span.
   */

  val defaultMinLifeTime = FiniteDuration(1, TimeUnit.HOURS)
  val defaultMaxLifeTime = FiniteDuration(8, TimeUnit.HOURS)

  /**
   * Defines a period of time within which the underlying PRNG should be reseeded.
   * @param minLifeTime minimum period of time to reseed
   * @param maxLifeTime maximum period of time to reseed
   * @throws IllegalArgumentException if min is less than max, as might be expected
   */
  case class TimeRangeToReseed(minLifeTime: FiniteDuration = defaultMinLifeTime, maxLifeTime: FiniteDuration = defaultMaxLifeTime) {
    require(minLifeTime < maxLifeTime, "minLifeTime must be less than maxLifeTime")
  }



  /**
   * Assembles Akka Props for the actor to avoid closing over actor state.
   * @param randomByteSource a random byte source that will be wrapped by the actor.
   * @param secureSeeder a secure seeder to use to fetch initial seeding of the random byte source and for subsequent reseeding
   * @return akka props
   */
  def props(randomByteSource: RandomSource, secureSeeder: SecureSeeder): Props = Props(new RandomSourceActor(randomByteSource, secureSeeder))


  /**
   * Assembles Akka Props for the actor.
   * @param randomByteSource a random byte source that will be wrapped by the actor.
   * @param secureSeeder a secure seeder to use to fetch initial seeding of the random byte source and for subsequent reseeding
   * @param timeRangeToReseed specified time range to reseed
   * @return akka props
   */
  def props(randomByteSource: RandomSource, secureSeeder: SecureSeeder, timeRangeToReseed: TimeRangeToReseed): Props =
    Props(new RandomSourceActor(randomSource = randomByteSource, secureSeeder = secureSeeder, timeRangeToReseed = timeRangeToReseed))

  /**
   * Utility function to find a random duration between the min and max of the configured time range which will
   * be used to schedule a reseeding of the underlying PRNG.
   * @param config determines the limits between which the point to reseed will lie
   * @param randomBytesource used to find a random point between the limits
   * @return a duration (de factor relative to "now") that effectively represents the point in time at which
   *         the reseeding should be scheduled to start
   */
  def computeScheduledTimeToReseed(config: TimeRangeToReseed, randomBytesource: RandomSource): FiniteDuration = {
    val actualDuration = config.maxLifeTime - config.minLifeTime
    val numberOfMillis = actualDuration.toMillis.asInstanceOf[Int]
    val randomInterval = randomBytesource.nextInt(numberOfMillis)
    config.minLifeTime + (randomInterval milliseconds)
  }



}
