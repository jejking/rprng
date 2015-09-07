package com.jejking.rprng.rng

import org.scalatest.{Matchers, FlatSpec}

/**
 * Tests for [[SecureRandomSeeder]].
 */
class SecureSeederSpec extends FlatSpec with Matchers {

  "the secure seeder" should "generate 64 bits of seed (a long) using generateSeed on the supplied SecureRandom instance" in {
    val secureRandom = new FixedSeedGeneratingSecureRandom()
    val secureSeeder = new SecureRandomSeeder(secureRandom)

    secureSeeder.generateSeed() shouldBe byteArrayToLong(Array[Byte](0, 0, 0, 0, 0, 0, 0, 0))

  }

  def byteArrayToLong(bytes: Array[Byte]): Long = {

    // from org.apache.hadoop.hbase.util.Bytes
    var l: Long = 0
    for (i <- 0 to 7) {
      l <<= 8
      l ^= bytes(i) & 0xFF
    }
    l
  }
}
