package com.jejking.rprng.rng

import org.apache.commons.math3.random.{MersenneTwister, RandomGenerator}
import org.apache.commons.math3.stat.Frequency
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, Matchers}

/**
 * Simple tests of [[RandomGeneratorSource]].
 */
class RandomGeneratorSourceSpec extends FlatSpec with Matchers with MockFactory {

  "a commons math byte source" should "generate request an array of bytes of correct size from underlying generator" in {

    val notVeryRandomBytes = Array[Byte](1, 2, 3, 4)

    val randomGenerator = mock[RandomGenerator]
    (randomGenerator.nextBytes _).expects(where {
      (ba: Array[Byte]) => ba.length == 4
    }).onCall((ba: Array[Byte]) => notVeryRandomBytes.copyToArray(ba))

    val byteSource = RandomGeneratorSource(randomGenerator)

    val actual = byteSource.randomBytes(RandomByteRequest(4))
    actual should be (notVeryRandomBytes)
  }


  it should "allow a reseeding to take place" in {
    val mockedGenerator = mock[RandomGenerator]
    (mockedGenerator.setSeed(_: Long)).expects(123L)

    val byteSourceWithMockGenerator = new RandomGeneratorSource(mockedGenerator)
    byteSourceWithMockGenerator.reseed(123L)
  }


  it should "reject a request for 0 bytes" in {

    val byteSource = RandomGeneratorSource(mock[RandomGenerator])
    intercept[IllegalArgumentException] {
      byteSource.randomBytes(RandomByteRequest(0))
    }
  }

  it should "reject a request for -1 bytes" in {

    val byteSource = RandomGeneratorSource(mock[RandomGenerator])
    intercept[IllegalArgumentException] {
      byteSource.randomBytes(RandomByteRequest(-1))
    }
  }

  it should "create a random long" in {
    val eightZeroes = Array[Byte](0, 0, 0, 0, 0, 0, 0, 0)
    val randomGenerator = mock[RandomGenerator]
    (randomGenerator.nextBytes _).expects(where {
      (ba: Array[Byte]) => ba.length == 8
    }).onCall((ba: Array[Byte]) => eightZeroes.copyToArray(ba))

    val source = RandomGeneratorSource(randomGenerator)

    val notVeryRandomLong = source.nextLong()

    notVeryRandomLong should be (0L)
  }


  it should "create a random int" in {
    val fourZeroes = Array[Byte]( 0, 0, 0, 0)
    val randomGenerator = mock[RandomGenerator]
    (randomGenerator.nextBytes _).expects(where {
      (ba: Array[Byte]) => ba.length == 4
    }).onCall((ba: Array[Byte]) => fourZeroes.copyToArray(ba))

    val source = RandomGeneratorSource(randomGenerator)

    val notVeryRandomInt = source.nextInt()

    notVeryRandomInt should be (0L)
  }

  it should "create a random int between zero and a positive int bound" in {
    // essentially just test that we call the backing mock, lic delegated to Apache Commons Math at the moment
    val randomGenerator = mock[RandomGenerator]
    (randomGenerator.nextInt (_:Int)).expects(10).returning(7)

    val source = RandomGeneratorSource(randomGenerator)
    val notVeryRandomInt = source.nextInt(10)

    notVeryRandomInt should be (7)
  }

  it should "respect the bounds between zero and the specified positive int bound" in {
    val randomGenerator = new MersenneTwister
    val source = RandomGeneratorSource(randomGenerator)
    val frequency = new Frequency()
    for (i <- 1 to 10000) {
      val randomInt = source.nextInt(10)
      assert(randomInt >= 0)
      assert(randomInt < 10)
      frequency.addValue(randomInt)
    }
    // rough check for proper distribution
    for (i <- 0 to 9) {
      frequency.getPct(i) should be (0.1 +- 0.01)
    }
  }

  it should "respect the bounds between min and max when requesting a customer range" in {
    val randomGenerator = new MersenneTwister
    val source = RandomGeneratorSource(randomGenerator)
    val frequency = new Frequency()
    for (i <- 1 to 10000) {
      val randomInt = source.nextInt(RandomIntRequest(11, 20))
      assert(randomInt >= 11)
      assert(randomInt <= 20)
      frequency.addValue(randomInt)
    }
    // rough check for proper distribution
    for (i <- 11 to 20) {
      frequency.getPct(i) should be (0.1 +- 0.01)
    }
  }

  it should "respect the bounds between min and max when using negative to positive range" in {
    val randomGenerator = new MersenneTwister
    val source = RandomGeneratorSource(randomGenerator)

    for (i <- 1 to 1000) {
      val randomInt = source.nextInt(RandomIntRequest(-10, 20))
      assert(randomInt >= -10)
      assert(randomInt <= 20)
    }
  }

  it should "respect the bounds between min and max when using range of negative integers" in {
    val randomGenerator = new MersenneTwister
    val source = RandomGeneratorSource(randomGenerator)

    for (i <- 1 to 1000) {
      val randomInt = source.nextInt(RandomIntRequest(-20, -10))
      assert(randomInt >= -20)
      assert(randomInt <= -10)
    }
  }

}





