package com.jejking.rprng.rng

import org.apache.commons.math3.random.MersenneTwister
import org.scalatest.{Matchers, FlatSpec}

/**
 * Tests factory.
 */
class RandomGeneratorFactorySpec extends FlatSpec with Matchers {

  "the factory" should "create a fresh instance of the requested PRNG" in {
    val mt: MersenneTwister = RandomGeneratorFactory.createNewGeneratorInstance[MersenneTwister]
    mt shouldBe a [MersenneTwister]
  }

}
