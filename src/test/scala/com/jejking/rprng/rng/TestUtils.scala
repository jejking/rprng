package com.jejking.rprng.rng

import akka.actor.Actor
import akka.util.ByteString

import java.security.SecureRandom

/**
 * Holds some useful test classes, constants and things.
 */
object TestUtils {

  def arrayOfEightZeroBytes(): Array[Byte] = Array.ofDim(8)
  val eightByteStringOfZeroes: EightByteString = EightByteString(ByteString.fromArray(arrayOfEightZeroBytes()))

  class InsecureSeeder extends SecureSeeder {
    /**
     * Generates a long. Implementations may block.
     * @return
     */
    override def generateSeed(): Seed = Seed(0L)
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

    override def reseed(seed: Seed): Unit = {}

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
