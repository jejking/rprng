package com.jejking.rprng.rng

import java.nio.ByteBuffer

import org.apache.commons.math3.random.RandomGenerator

/**
 * Defines functionality to generate random bytes and to set
 * random seed for the underlying implementation.
 */
trait RandomByteSource {

  /**
   * Creates random bytes into a newly created array of the size specified in the request.
   * @param request request for random bytes. Request size must be strictly greater than zero.
   * @return freshly created array filled iwith the requested amount of randomness
   */
  def randomBytes(request: RandomByteRequest): Array[Byte]

  /**
   * Supplies eight bytes worth of high-quality entropy to reseed the PRNG.
   * @param seed long, hopefully quite random.
   */
  def reseed(seed: Long): Unit

  /**
   * Requests 8 bytes of randomness and uses these to construct a long.
   * @return a random long
   */
  def nextLong(): Long = {
    val bytes = randomBytes(RandomByteRequest(8))
    val wrapper = ByteBuffer.wrap(bytes)
    wrapper.getLong
  }

  /**
   * Creates a random integer between zero and the supplied upper bound.
   * @param bound the upper bound.
   * @return a random integer
   */
  def nextInt(bound: Int): Int
}

/**
 * Implementation based on Apache Commons Math.
 * @param randomGenerator the Apache Commons Math generator to use.
 */
class RandomGeneratorByteSource(val randomGenerator: RandomGenerator) extends RandomByteSource {

  override def randomBytes(request: RandomByteRequest): Array[Byte] = {
    require(request.count >= 1, "Requested byte array size must be strictly positive")
    val theArray = new Array[Byte](request.count)
    randomGenerator.nextBytes(theArray)
    theArray
  }

  override def reseed(seed: Long): Unit = {
    randomGenerator.setSeed(seed)
  }

  override def nextInt(bound: Int): Int = {
    randomGenerator.nextInt(bound)
  }

}

/**
 * Request for a given number of bytes.
 * @param count number off bytes to request, must be strictly positive.
 */
case class RandomByteRequest(count: Int) extends AnyVal

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