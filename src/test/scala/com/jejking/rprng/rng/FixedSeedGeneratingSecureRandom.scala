package com.jejking.rprng.rng

import java.security.SecureRandom

/**
 * Dummy class that generates well known seed.
 */
class FixedSeedGeneratingSecureRandom extends SecureRandom {

  private val fixedByteSource = new CommonsMathRandomByteSource(FixedApacheRandomGenerator())

  override def generateSeed(numBytes: Int) =  {
    fixedByteSource.randomBytes(numBytes)
  }
}
