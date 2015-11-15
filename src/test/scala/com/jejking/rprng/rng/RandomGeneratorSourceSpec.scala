package com.jejking.rprng.rng

import org.apache.commons.math3.random.RandomGenerator
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
    // essentially just test that we call the backing mock, logic delegated to Apache Commons Math at the moment
    val randomGenerator = mock[RandomGenerator]
    (randomGenerator.nextInt (_:Int)).expects(10).returning(7)

    val source = RandomGeneratorSource(randomGenerator)
    val notVeryRandomInt = source.nextInt(10)

    notVeryRandomInt should be (7)
  }
}





