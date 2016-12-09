package com.jejking.rprng.rng

import java.nio.ByteBuffer
import java.security.SecureRandom

/**
 * Defines functionality to generate 8 bytes worth of secure seed.
 */
trait SecureSeeder {

  /**
   * Generates a long. Implementations may block. It is therefore good practice to *not*
   * call this method in the receive block of an Actor.
   * @return long with 8 bytes of high quality randomness
   */
  def generateSeed(): Seed
}

/**
 * Class to provide high-quality random seed for the PRNGs on the basis.
 * @param secureRandom underlying source as supplied by the JVM infrastructure.
 */
class SecureRandomSeeder(private val secureRandom: SecureRandom) extends SecureSeeder {

  /**
   * Generates 8 bytes of randomness and converts these to a Long. The method
   * will block.
   *
   * @return a random long derived from the `generateSeed()` method of `SecureRandom`
   */
  override def generateSeed(): Seed = {
    val bytes = secureRandom.generateSeed(8)
    val wrapper = ByteBuffer.wrap(bytes)
    Seed(wrapper.getLong)
  }

}


