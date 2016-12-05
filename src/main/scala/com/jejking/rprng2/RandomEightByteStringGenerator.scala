package com.jejking.rprng2

import akka.util.ByteString
import org.apache.commons.math3.random.RandomGenerator

/**
  * Defines functionality to generate random byte strings of length 8.
  */
trait RandomEightByteStringGenerator {

  /**
    * Generates a random byte string of length 8.
    *
    * @return an [[EightByteString]]
    */
  def randomEightByteString(): EightByteString

  /**
    * Supplies some new random seed for an underlying PRNG.
    * @param seed
    */
  def reseed(seed: Long): Unit

}

class CommonsMathRandomEightByteStringGenerator(randomGenerator: RandomGenerator) extends RandomEightByteStringGenerator {

  override def randomEightByteString(): EightByteString = {
    val theArray = new Array[Byte](8)
    randomGenerator.nextBytes(theArray)
    EightByteString(ByteString(theArray))
  }

  override def reseed(seed: Long): Unit = {
    randomGenerator.setSeed(seed)
  }
}
