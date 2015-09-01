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

  /**
   * Returns the class of the underlying RNG or other generator.
   * @return class
   */
  def generatorClass(): Class[_]
}

/**
 * Implementation of [[RandomByteSource]] based around the Apache Commons Math Library.
 * @param randomGenerator an instance that supplies the underlying randomness.
 */
class CommonsMathRandomByteSource(val randomGenerator: RandomGenerator) extends RandomByteSource {

  override def randomBytes(count: Int): Array[Byte] = {
    require(count > 0, "Requested byte array size must be strictly positive")
    val theArray = new Array[Byte](count)
    randomGenerator.nextBytes(new Array[Byte](count))
    theArray
  }

  override def generatorClass(): Class[_] = {
    randomGenerator.getClass
  }

}

case class RandomByteRequest(count: Int)

/**
 * Companion object with constructor helper.
 */
object CommonsMathRandomByteSource {

  /**
   * Constructs new [[CommonsMathRandomByteSource]] using supplied generator.
   * @param randomGenerator
   * @return freshly instantiated generator
   */
  def apply(randomGenerator: RandomGenerator): CommonsMathRandomByteSource = new CommonsMathRandomByteSource(randomGenerator)
}