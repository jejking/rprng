package com.jejking.rprng.rng

import org.apache.commons.math3.random.MersenneTwister
import org.scalatest.{Matchers, FlatSpec}

/**
 * Tests for [[RandomByteSourceFactory]].
 */
class RandomByteSourceFactorySpec extends FlatSpec with Matchers {

  "the factory" should "supply seed from a fixed instance of Secure Random (actually not that secure)" in {

    val secureRandom = new FixedSeedGeneratingSecureRandom()
    val factory = new RandomByteSourceFactory(secureRandom)

    val expected = Array[Byte](0, 0, 0 ,0)

    val actual = factory.generateSeed(4)

    actual shouldBe expected
  }

  it should "throw an IllegalArgumentException if zero or negative amount of seed requested" in {

    val secureRandom = new FixedSeedGeneratingSecureRandom()
    val factory = new RandomByteSourceFactory(secureRandom)

    intercept[IllegalArgumentException] {
      factory.generateSeed(0)
    }

    intercept[IllegalArgumentException] {
      factory.generateSeed(-1)
    }
  }

  it should "set 64 bits of seed (a long) on an instance of RandomGenerator" in {
    val secureRandom = new FixedSeedGeneratingSecureRandom()
    val factory = new RandomByteSourceFactory(secureRandom)

    val observableSeedRgn = new RandomGeneratorWithObservableSeed()

    factory.seedGenerator(observableSeedRgn )

    observableSeedRgn.seed shouldBe byteArrayToLong(Array[Byte](0, 0, 0, 0, 0, 0, 0, 0))

  }

  it should "create an instance of mersenne twister when asked to" in {

    val secureRandom = new FixedSeedGeneratingSecureRandom()
    val factory = new RandomByteSourceFactory(secureRandom)

    val mersenneTwister = factory.createNewGeneratorInstance[MersenneTwister]

    mersenneTwister shouldBe a [MersenneTwister]
  }

  it should "deliver a properly seeded commons math based byte source factory when asked to" in {

    val secureRandom = new FixedSeedGeneratingSecureRandom()
    val factory = new RandomByteSourceFactory(secureRandom)

    val observableSeedRgSource = factory.createRandomByteSource[RandomGeneratorWithObservableSeed]

    observableSeedRgSource
      .asInstanceOf[CommonsMathRandomByteSource]
      .randomGenerator
      .asInstanceOf[RandomGeneratorWithObservableSeed]
      .seed shouldBe byteArrayToLong(Array[Byte](0, 0, 0, 0, 0, 0, 0, 0))

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

