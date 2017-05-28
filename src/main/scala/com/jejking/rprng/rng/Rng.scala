package com.jejking.rprng.rng


/**
  * Request for a given number of bytes.
  * @param count number off bytes to request, must be strictly positive.
  */
case class RandomByteRequest(count: Int) extends AnyVal


/**
  * Request for an integer between min and max bound.
  * @param minBound inclusive minimum bound. Must be smaller than the maxBound
  * @param maxBound inclusive maximum bound. Must be greater than minBound.
  * @throws java.lang.IllegalArgumentException if preconditions not met
  */
case class RandomIntRequest(minBound: Int = 0, maxBound: Int) {
  require(minBound < maxBound, "minBound must be less than maxBound")
}

/**
 * Defines functionality to generate random bytes and to set
 * random seed for the underlying implementation.
 */
trait Rng {

  /**
   * Creates random bytes into a newly created array of the size specified in the request.
   * @param request request for random bytes. Request size must be strictly greater than zero.
   * @return freshly created array filled with the requested amount of randomness.
   */
  def randomBytes(request: RandomByteRequest): Array[Byte]

  /**
   * Supplies eight bytes worth of high-quality entropy to reseed the PRNG.
   * @param seed long, hopefully quite random.
   */
  def reseed(seed: Long): Unit


}



