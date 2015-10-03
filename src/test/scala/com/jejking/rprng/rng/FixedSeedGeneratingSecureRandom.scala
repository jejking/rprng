package com.jejking.rprng.rng

import java.security.SecureRandom

/**
 * Test class that generates zeroes.
 */
class FixedSeedGeneratingSecureRandom extends SecureRandom {

  override def generateSeed(numBytes: Int): Array[Byte] =  {
    new Array[Byte](numBytes)
  }
}
