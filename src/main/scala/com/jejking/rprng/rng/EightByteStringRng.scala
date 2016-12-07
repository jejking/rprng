package com.jejking.rprng.rng

import org.apache.commons.math3.random.RandomGenerator

/**
  * Defines minimum functionality for a synchronous pseudo-RNG. Implementations
  * are not expected to be thread-safe.
  */
trait EightByteStringRng {

  /**
    * Generates a new, (pseudo)-random [[EightByteString]] for
    * subsequent processing.
    *
    * @return random eight byte string
    */
  def randomEightByteString(): EightByteString

  /**
    * Supplies new seed to be used at the discretion of the implementation.
    * @param seed new seed. Should be supplied from a good source of randomness.
    */
  def seed(seed: Long): Unit
}


