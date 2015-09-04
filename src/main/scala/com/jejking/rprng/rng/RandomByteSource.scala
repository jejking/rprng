package com.jejking.rprng.rng

import org.apache.commons.math3.random.RandomGenerator

class RandomByteSource(val randomGenerator: RandomGenerator) {

  def randomBytes(request: RandomByteRequest): Array[Byte] = {
    val theArray = new Array[Byte](request.count)
    randomGenerator.nextBytes(new Array[Byte](request.count))
    theArray
  }

  def generatorClass(): Class[_] = {
    randomGenerator.getClass
  }

  def reseed(seed: Long): Unit = {
    randomGenerator.setSeed(seed)
  }

}

/**
 * Request for a given number of bytes.
 * @param count number off bytes to request, must be strictly positive.
 */
case class RandomByteRequest(count: Int) {
  require(count >= 1, "Requested byte array size must be strictly positive")
}

/**
 * Companion object with constructor helper.
 */
object RandomByteSource {

  /**
   * Constructs new [[RandomByteSource]] using supplied generator.
   * @param randomGenerator the underlying generator to use
   * @return freshly instantiated random byte source
   */
  def apply(randomGenerator: RandomGenerator): RandomByteSource = new RandomByteSource(randomGenerator)
}