package com.jejking.rprng.rng

import org.apache.commons.math3.random.RandomGenerator

/**
 * Defines functionality to obtain a fixed number of random bytes.
 */
trait RandomByteSource {

  /**
   * Obtains the requested number of bytes.
   * @param count number of bytes. Must be 1 or greater.
   * @return corresponding array of bytes
   */
  def randomBytes(count: Int): Array[Byte]
}

class CommonsMathRandomByteSource(randomGenerator: RandomGenerator) extends RandomByteSource {

  override def randomBytes(count: Int): Array[Byte] = {
    require(count > 0, "Requested byte array size must be strictly positive")
    val theArray = new Array[Byte](count)
    randomGenerator.nextBytes(new Array[Byte](count))
    theArray
  }

}

object CommonsMathRandomByteSource {

  def apply(randomGenerator: RandomGenerator): CommonsMathRandomByteSource = new CommonsMathRandomByteSource(randomGenerator)
}