package com.jejking.rprng.rng

import java.nio.ByteBuffer
import java.security.SecureRandom

/**
 * Functionality to generate secure seed.
 */
trait SecureSeeder {

  /**
   * Generates a long. Implementations may block.
   * @return
   */
  def generateSeed(): Long
}

/**
 * Class to provide high-quality random seed for the PRNGs on the basis.
 * @param secureRandom underlying source as supplied by the JVM infrastructure.
 */
class SecureRandomSeeder(private val secureRandom: SecureRandom) extends SecureSeeder {

  /**
   * Generates 8 bytes of randomness and converts these to a Long. The method
   * is likely to block.
   *
   * @return a random long derived from the `generateSeed()` method of `SecureRandom`
   */
  override def generateSeed(): Long = {
    val bytes = secureRandom.generateSeed(8)
    val wrapper = ByteBuffer.wrap(bytes)
    wrapper.getLong
  }

}


