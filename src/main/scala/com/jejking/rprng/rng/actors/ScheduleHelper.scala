package com.jejking.rprng.rng.actors

import akka.actor.Cancellable
import com.jejking.rprng.rng.{RandomEightByteStringGenerator}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

import scala.language.postfixOps

/**
 * Isolates the functionality we need from the Akka Scheduler.
 */
trait ScheduleHelper {
  def scheduleOnce(delay: FiniteDuration)(f: ⇒ Unit)(implicit executor: ExecutionContext): Cancellable

}

object ScheduleHelper {

  import com.jejking.rprng.rng.EightByteStringOps.toInt

  /**
    * Utility function to find a random duration between the min and max of the configured time range which will
    * be used to schedule a reseeding of the underlying PRNG.*
    * @param config determines the limits between which the point to reseed will lie
    * @param randomEightByteStringGenerator used to find a random point between the limits
    * @return a duration (de facto relative to "now") that effectively
    *         represents the point in time at which the reseeding should be scheduled to start
    */
  def computeScheduledTimeToReseed(config: TimeRangeToReseed, randomEightByteStringGenerator: RandomEightByteStringGenerator): FiniteDuration = {
    val actualDuration = config.maxLifeTime - config.minLifeTime
    val numberOfMillis = actualDuration.toMillis.asInstanceOf[Int]
    val randomInterval = toInt(randomEightByteStringGenerator.randomEightByteString())
    config.minLifeTime + (randomInterval milliseconds)
  }
}
