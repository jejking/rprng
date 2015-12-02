package com.jejking.rprng.rng

import java.nio.ByteBuffer

import org.apache.commons.math3.random.RandomGenerator

/**
 * Defines functionality to generate random bytes and to set
 * random seed for the underlying implementation.
 */
trait RandomSource {

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
   * Creates a random integer between zero (inclusive) and the supplied upper bound (exclusive).
   * @param bound the upper bound.
   * @return a random integer
   */
  def nextInt(bound: Int): Int

  /**
   * Creates a random integer between the lower bound (inclusive) and the upper bound (inclusive).
   * @param req request specifying bounds
   * @return a random integer
   */
  def nextInt(req: RandomIntRequest): Int = {
    if (req.minBound == Int.MinValue && req.maxBound == Int.MaxValue) {
      nextInt()
    } else {
      nextInt((req.maxBound - req.minBound) + 1) + req.minBound
    }
  }


  /**
   * Requests four bytes of randomness and uses these to construct an int.
   * @return a random int
   */
  def nextInt(): Int = {
    val bytes = randomBytes(RandomByteRequest(4))
    val wrapper = ByteBuffer.wrap(bytes)
    wrapper.getInt
  }
}

/**
 * Implementation based on Apache Commons Math.
 * @param randomGenerator the Apache Commons Math generator to use.
 */
class RandomGeneratorSource(val randomGenerator: RandomGenerator) extends RandomSource {

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
    require(bound > 0, s"Bound must be strictly positive, but was $bound")
    randomGenerator.nextInt(bound)
  }

}

/**
 * Request for a given number of bytes.
 * @param count number off bytes to request, must be strictly positive.
 */
case class RandomByteRequest(count: Int) extends AnyVal

/**
 * Request for any integer.
 */
case object RandomAnyIntRequest

/**
 * Request for an integer between min and max bound.
 * @param minBound inclusive minimum bound. Must be smaller than the maxBound
 * @param maxBound inclusive maximum bound. Must be greater than minBound.
 * @throws IllegalArgumentException if preconditions not met
 */
case class RandomIntRequest(minBound: Int = 0, maxBound: Int) {
  require(minBound < maxBound, "minBound must be less than maxBound")
}


/**
 * Companion object with constructor helper.
 */
object RandomGeneratorSource {

  /**
   * Constructs new [[RandomGeneratorSource]] using supplied generator.
   * @param randomGenerator the underlying generator to use
   * @return freshly instantiated random byte source
   */
  def apply(randomGenerator: RandomGenerator): RandomGeneratorSource = new RandomGeneratorSource(randomGenerator)
}