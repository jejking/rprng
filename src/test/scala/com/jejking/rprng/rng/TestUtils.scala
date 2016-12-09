package com.jejking.rprng.rng

import java.security.SecureRandom

import akka.actor.Actor
import akka.util.ByteString

/**
 * Holds some useful test classes, constants and things.
 */
object TestUtils {

  def arrayOfEightZeroBytes(): Array[Byte] = Array.ofDim(8)
  val eightByteStringOfZeroes: EightByteString = EightByteString(ByteString.fromArray(arrayOfEightZeroBytes))

  class InsecureSeeder extends SecureSeeder {
    /**
     * Generates a long. Implementations may block.
     * @return
     */
    override def generateSeed(): Long = 0L
  }

  /**
    * Test class that generates zeroes.
    */
  class FixedSeedGeneratingSecureRandom extends SecureRandom {

    override def generateSeed(numBytes: Int): Array[Byte] =  {
      new Array[Byte](numBytes)
    }
  }


  class ZeroRng extends Rng {
    override def randomBytes(request: RandomByteRequest): Array[Byte] = Array.ofDim(8)

    override def reseed(seed: Long): Unit = {}

    override def nextInt(bound:Int): Int = 0
  }

  class ZeroEightByteStringRng extends RandomEightByteStringGenerator {

    /**
      * Generates a new, (pseudo)-random [[EightByteString]] for
      * subsequent processing.
      *
      * @return random eight byte string
      */
    override val randomEightByteString = TestUtils.eightByteStringOfZeroes


    /**
      * Supplies new seed to be used at the discretion of the implementation.
      *
      * @param seed new seed. Should be supplied from a good source of randomness.
      */
    override def seed(seed: Seed): Unit = {}
  }


  class FailureActor extends Actor {
    override def receive: Actor.Receive = {
      case _ => sender() ! akka.actor.Status.Failure(new RuntimeException("I hereby fail miserably"))
    }
  }

  def byteStringOfZeroes(size: Int): ByteString = {
    val byteStringBuilder = ByteString.newBuilder
    for (i <- 1 to size) {
      byteStringBuilder.putByte(0)
    }
    byteStringBuilder.result()
  }


  val oneKb = byteStringOfZeroes(1024)
  val twoKb = oneKb ++ oneKb

}
