package com.jejking.rprng.rng

import java.security.SecureRandom

/**
 * Dummy class that generates well known seed.
 */
class FixedSeedGeneratingSecureRandom extends SecureRandom {

  private val fixedByteSource = new RandomGeneratorByteSource(FixedApacheRandomGenerator())

  override def generateSeed(numBytes: Int) =  {
    fixedByteSource.randomBytes(RandomByteRequest(numBytes))
  }
}
