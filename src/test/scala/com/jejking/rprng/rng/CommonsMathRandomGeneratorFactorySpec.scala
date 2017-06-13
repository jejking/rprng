package com.jejking.rprng.rng

import org.apache.commons.math3.random.MersenneTwister
import org.scalatest.{FlatSpec, Matchers}

/**
 * Tests factory.
 */
class CommonsMathRandomGeneratorFactorySpec extends FlatSpec with Matchers {

  "the factory" should "create a fresh instance of the requested PRNG" in {
    val mt: MersenneTwister = CommonsMathRandomGeneratorFactory.createNewGeneratorInstance[MersenneTwister]
    mt shouldBe a [MersenneTwister]
  }

}