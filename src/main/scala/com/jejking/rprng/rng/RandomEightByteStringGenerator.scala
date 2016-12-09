package com.jejking.rprng.rng

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
  def seed(seed: Seed): Unit

}

class CommonsMathRandomEightByteStringGenerator(randomGenerator: RandomGenerator) extends RandomEightByteStringGenerator {

  override def randomEightByteString(): EightByteString = {
    val theArray = new Array[Byte](8)
    randomGenerator.nextBytes(theArray)
    EightByteString(ByteString(theArray))
  }

  override def seed(seed: Seed): Unit = {
    randomGenerator.setSeed(seed.seed)
  }
}

/**
  * Case object encapsulating a request for an [[EightByteString]] for
  * example in an actor.
  */
case object EightByteStringRequest

/**
  * Tiny type encapsulating new seed for a PRNG.
  * @param seed
  */
case class Seed(seed: Long) extends AnyVal