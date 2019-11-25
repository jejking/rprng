package com.jejking.rprng.rng

import org.apache.commons.math3.random.RandomGenerator
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, Matchers}

/**
 * Simple tests of [[CommonsMathRng]].
 */
class CommonsMathRngSpec extends FlatSpec with Matchers with MockFactory {

  "a commons math byte source" should "generate request an array of bytes of correct size from underlying generator" in {

    val notVeryRandomBytes = Array[Byte](1, 2, 3, 4)

    val randomGenerator = mock[RandomGenerator]
    (randomGenerator.nextBytes _).expects(where {
      (ba: Array[Byte]) => ba.length == 4
    }).onCall((ba: Array[Byte]) => {
      notVeryRandomBytes.copyToArray(ba)
      ()
    })

    val byteSource = CommonsMathRng(randomGenerator)

    val actual = byteSource.randomBytes(RandomByteRequest(4))
    actual should be (notVeryRandomBytes)
  }


  it should "allow a reseeding to take place" in {
    val mockedGenerator = mock[RandomGenerator]
    (mockedGenerator.setSeed(_: Long)).expects(123L)

    val byteSourceWithMockGenerator = new CommonsMathRng(mockedGenerator)
    byteSourceWithMockGenerator.reseed(Seed(123L))
  }


  it should "reject a request for 0 bytes" in {

    val byteSource = CommonsMathRng(mock[RandomGenerator])
    intercept[IllegalArgumentException] {
      byteSource.randomBytes(RandomByteRequest(0))
    }
  }

  it should "reject a request for -1 bytes" in {

    val byteSource = CommonsMathRng(mock[RandomGenerator])
    intercept[IllegalArgumentException] {
      byteSource.randomBytes(RandomByteRequest(-1))
    }
  }

}





