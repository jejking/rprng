package com.jejking.rprng.rng.actors

import java.util.concurrent.TimeUnit

import akka.util.ByteString
import com.jejking.rprng.rng.{EightByteString, RandomByteRequest, Rng}

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Defines a period of time within which the underlying PRNG should be reseeded.
  * @param minLifeTime minimum period of time to reseed
  * @param maxLifeTime maximum period of time to reseed
  * @throws java.lang.IllegalArgumentException if min is less than max, as might be expected
  */
case class TimeRangeToReseed(minLifeTime: FiniteDuration = TimeRangeToReseed.defaultMinLifeTime, maxLifeTime: FiniteDuration = TimeRangeToReseed.defaultMaxLifeTime) {
  require(minLifeTime < maxLifeTime, "minLifeTime must be less than maxLifeTime")
}

object TimeRangeToReseed {
  /**
    * Default time to reseed is between one and eight hours, a completely arbitrary time span.
    */

  val defaultMinLifeTime = FiniteDuration(1, TimeUnit.HOURS)
  val defaultMaxLifeTime = FiniteDuration(8, TimeUnit.HOURS)

  /**
    * Utility function to find a random duration between the min and max of the configured time range which will
    * be used to schedule a reseeding of the underlying PRNG.
    *
    * @param config determines the limits between which the point to reseed will lie
    * @param rng used to find a random point between the limits
    * @return a duration (de facto from "now") that effectively represents the point
    *         in time at which the reseeding should be scheduled to start
    */
  def durationToReseed(config: TimeRangeToReseed, rng: Rng): FiniteDuration = {
    import com.jejking.rprng.rng.EightByteStringOps.toInt

    val actualDuration = config.maxLifeTime - config.minLifeTime
    val numberOfMillis = actualDuration.toMillis.asInstanceOf[Int]
    val randomInterval = toInt(EightByteString(ByteString(rng.randomBytes((RandomByteRequest(8))))), numberOfMillis)
    config.minLifeTime + (randomInterval milliseconds)
  }
}