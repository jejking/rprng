package com.jejking.rprng.rng

import akka.actor.Actor
import akka.util.ByteString

/**
 * Holds some useful test classes and things.
 */
object TestUtils {

  class InsecureSeeder extends SecureSeeder {
    /**
     * Generates a long. Implementations may block.
     * @return
     */
    override def generateSeed(): Long = 0L
  }

  class ZeroRandomSource extends RandomSource {
    override def randomBytes(request: RandomByteRequest): Array[Byte] = Array(0.toByte, 0.toByte, 0.toByte, 0.toByte, 0.toByte, 0.toByte, 0.toByte, 0.toByte)

    override def reseed(seed: Long): Unit = {}

    override def nextInt(bound:Int): Int = 0
  }

  class FailureActor extends Actor {
    override def receive: Actor.Receive = {
      case _ => sender() ! akka.actor.Status.Failure(new RuntimeException("I fail"))
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
