package com.jejking.rprng.rng

import org.apache.commons.math3.random.RandomGenerator
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, FlatSpec}

/**
 * Simple tests of [[RandomGeneratorByteSource]].
 */
class RandomByteSourceSpec extends FlatSpec with Matchers with MockFactory {

  val byteSource =   new RandomGeneratorByteSource(FixedApacheRandomGenerator())

  "a commons math byte source" should "generate an array of 4 fixed bytes with the fixed generator" in {

    val expected = Array[Byte](0, 0, 0, 0)
    val actual = byteSource.randomBytes(RandomByteRequest(4))
    actual should be (expected)
  }

  it should "generate an array of 1 fixed byte with the fixed generator" in {
    val expected = Array[Byte](0)
    val actual = byteSource.randomBytes(RandomByteRequest(1))
    actual should be (expected)
  }

  it should "allow a reseeding to take place" in {
    val mockedGenerator = mock[RandomGenerator]
    (mockedGenerator.setSeed(_: Long)).expects(123L)

    val byteSourceWithMockGenerator = new RandomGeneratorByteSource(mockedGenerator)
    byteSourceWithMockGenerator.reseed(123L)
  }

}

class RandomByteRequestSpec extends FlatSpec with Matchers {

  "the case class" should "accept a value of 1" in {
    val req = RandomByteRequest(1)
    req.count should be (1)
  }

  it should "reject a value of 0" in {
    intercept[IllegalArgumentException] {
      RandomByteRequest(0)
    }
  }

  it should "reject a value of -1" in {
    intercept[IllegalArgumentException] {
      RandomByteRequest(-1)
    }
  }


}




