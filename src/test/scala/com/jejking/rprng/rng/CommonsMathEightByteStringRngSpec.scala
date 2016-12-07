package com.jejking.rprng.rng

import akka.util.ByteString
import org.apache.commons.math3.random.RandomGenerator
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, Matchers}

/**
  * Simple unit tests for [[CommonsMathEightByteStringRng]].
  */
class CommonsMathEightByteStringRngSpec extends FlatSpec with Matchers with MockFactory {

  "CommonsMathEightByteStringRng" should "obtain eight bytes of randomness from the backing generator" in {
    val notVeryRandomBytes = Array[Byte](1, 2, 3, 4, 5, 6, 7, 8)

    val randomGenerator = mock[RandomGenerator]
    (randomGenerator.nextBytes _).expects(where {
      (ba: Array[Byte]) => ba.length == 8
    }).onCall((ba: Array[Byte]) => notVeryRandomBytes.copyToArray(ba))

    val byteSource = CommonsMathEightByteStringRng(randomGenerator)

    val actual = byteSource.randomEightByteString()
    actual.byteString shouldBe ByteString(notVeryRandomBytes)
  }

  it should "reseed the backing generator when supplied with new seed" in {
    val randomGenerator = mock[RandomGenerator]
    (randomGenerator.setSeed(_: Long)).expects(123L)

    val byteSource = CommonsMathEightByteStringRng(randomGenerator)
    byteSource.seed(123L)
  }

}
