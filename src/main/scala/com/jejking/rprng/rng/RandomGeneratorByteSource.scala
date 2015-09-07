package com.jejking.rprng.rng

import org.apache.commons.math3.random.RandomGenerator

/**
 * Defines functionality to generate random bytes and to set
 * random seed for the underlying implementation.
 */
trait RandomByteSource {

  def randomBytes(request: RandomByteRequest): Array[Byte]

  def reseed(seed: Long): Unit
}

class RandomGeneratorByteSource(val randomGenerator: RandomGenerator) extends RandomByteSource {

  override def randomBytes(request: RandomByteRequest): Array[Byte] = {
    val theArray = new Array[Byte](request.count)
    randomGenerator.nextBytes(new Array[Byte](request.count))
    theArray
  }

  override def reseed(seed: Long): Unit = {
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
object RandomGeneratorByteSource {

  /**
   * Constructs new [[RandomGeneratorByteSource]] using supplied generator.
   * @param randomGenerator the underlying generator to use
   * @return freshly instantiated random byte source
   */
  def apply(randomGenerator: RandomGenerator): RandomGeneratorByteSource = new RandomGeneratorByteSource(randomGenerator)
}