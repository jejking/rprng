package com.jejking.rprng.rng.actors

import com.jejking.rprng.rng._
import org.apache.commons.math3.random.MersenneTwister
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by jking on 09/12/2016.
  */
class TimeRangeToReseedSpec extends AnyFlatSpec with Matchers with MockFactory {

  "TimeRangeToReseed" should "provide a default min and max lifetime" in {
    val lifeSpanRange = TimeRangeToReseed()
    lifeSpanRange.minLifeTime shouldBe TimeRangeToReseed.defaultMinLifeTime
    lifeSpanRange.maxLifeTime shouldBe TimeRangeToReseed.defaultMaxLifeTime
  }

  it should "accept input where min is less than max" in {
    TimeRangeToReseed(1 minute, 2 minutes)
  }

  it should "reject input where min is greater than max" in {
    intercept[IllegalArgumentException] {
      TimeRangeToReseed(1 hour, 1 minute)
    }
  }

  it should "reject input where min is equal to max" in {
    intercept[IllegalArgumentException] {
      TimeRangeToReseed(1 minute, 1 minute)
    }
  }

  "the companion object" should "compute an appropriate schedule" in {
    val byteSource = stub[Rng]
    (byteSource.randomBytes _).when(*).returns(TestUtils.arrayOfEightZeroBytes())
    val minLifeTime: FiniteDuration = 1 minute
    val maxLifeTime: FiniteDuration = 2 minutes

    val lifeSpanRange = TimeRangeToReseed(minLifeTime, maxLifeTime)
    TimeRangeToReseed.durationToReseed(lifeSpanRange, byteSource) shouldBe (1 minute)

    val mersenneTwister = new MersenneTwister()

    for (i <- 1 to 100) {
      val computedScheduledTimeOfDeath = TimeRangeToReseed.durationToReseed(lifeSpanRange,
        new CommonsMathRng(mersenneTwister))
      assert(computedScheduledTimeOfDeath >= minLifeTime)
      assert(computedScheduledTimeOfDeath <= maxLifeTime)
    }
  }
}
