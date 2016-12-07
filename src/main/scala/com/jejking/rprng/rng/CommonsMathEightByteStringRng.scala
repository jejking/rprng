package com.jejking.rprng.rng

import akka.util.ByteString
import org.apache.commons.math3.random.RandomGenerator

/**
  * Implementation class that delegates to a [[RandomGenerator]] implementation from Apache Commons Math.
  *
  * @param randomGenerator
  */
class CommonsMathEightByteStringRng(private val randomGenerator: RandomGenerator) extends EightByteStringRng {

  /**
    * Generates a new, (pseudo)-random [[EightByteString]] for
    * subsequent processing.
    *
    * @return random eight byte string
    */
  override def randomEightByteString(): EightByteString = {
    val byteArray: Array[Byte] = Array.ofDim(8)
    randomGenerator.nextBytes(byteArray)
    EightByteString(ByteString(byteArray))
  }

  /**
    * Supplies new seed to be used at the discretion of the implementation.
    *
    * @param seed new seed. Should be supplied from a good source of randomness.
    */
  override def seed(seed: Long): Unit = {
    randomGenerator.setSeed(seed)
  }
}

object CommonsMathEightByteStringRng {

  def apply(randomGenerator: RandomGenerator) = {
    new CommonsMathEightByteStringRng(randomGenerator)
  }

}