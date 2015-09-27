package com.jejking.rprng.rng

import java.security.SecureRandom

/**
 * Dummy class that generates well known seed.
 */
class FixedSeedGeneratingSecureRandom extends SecureRandom {

  override def generateSeed(numBytes: Int): Array[Byte] =  {
    new Array[Byte](numBytes)
  }
}
