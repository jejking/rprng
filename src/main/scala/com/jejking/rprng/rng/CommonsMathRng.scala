package com.jejking.rprng.rng

import org.apache.commons.math3.random.RandomGenerator

/**
  * Implementation based on Apache Commons Math.
  * @param randomGenerator the Apache Commons Math generator to use.
  */
class CommonsMathRng(val randomGenerator: RandomGenerator) extends Rng {

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
  * Companion object with constructor helper.
  */
object CommonsMathRng {

  /**
    * Constructs new [[CommonsMathRng]] using supplied generator.
    *
    * @param randomGenerator the underlying generator to use
    * @return freshly instantiated random byte source
    */
  def apply(randomGenerator: RandomGenerator): CommonsMathRng = new CommonsMathRng(randomGenerator)
}