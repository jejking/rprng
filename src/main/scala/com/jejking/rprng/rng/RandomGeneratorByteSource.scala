package com.jejking.rprng.rng

import java.nio.ByteBuffer

import org.apache.commons.math3.random.RandomGenerator

/**
 * Defines functionality to generate random bytes and to set
 * random seed for the underlying implementation.
 */
trait RandomByteSource {

  def randomBytes(request: RandomByteRequest): Array[Byte]

  def reseed(seed: Long): Unit

  def nextLong(): Long = {
    val bytes = randomBytes(RandomByteRequest(8))
    val wrapper = ByteBuffer.wrap(bytes)
    wrapper.getLong
  }

  def nextDouble(): Double = {
    val bytes = randomBytes(RandomByteRequest(8))
    val wrapper = ByteBuffer.wrap(bytes)
    wrapper.getDouble
  }

  def nextInt(): Int = {
    val bytes = randomBytes(RandomByteRequest(4))
    val wrapper = ByteBuffer.wrap(bytes)
    wrapper.getInt
  }

  def nextInt(bound: Int): Int = {
    require(bound >= 1, "Bound must be strictly positive")
    // basically scala-ified version of the code from AbstractRandomGenerator.
    val result: Int = (nextDouble * bound).toInt
    return if (result < bound) result else bound - 1
  }
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