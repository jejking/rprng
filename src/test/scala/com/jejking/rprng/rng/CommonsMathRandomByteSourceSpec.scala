package com.jejking.rprng.rng

import org.scalatest.{Matchers, FlatSpec}

/**
 * Simple tests of [[CommonsMathRandomByteSource]].
 */
class CommonsMathRandomByteSourceSpec extends FlatSpec with Matchers {

  val byteSource = new CommonsMathRandomByteSource(FixedApacheRandomGenerator())

  "a commons math byte source" should "generate an array of 4 fixed bytes with the fixed generator" in {

    val expected = Array[Byte](0, 0, 0, 0)
    val actual = byteSource.randomBytes(4)
    actual should be (expected)
  }

  it should "generate an array of 1 fixed byte with the fixed generator" in {
    val expected = Array[Byte](0)
    val actual = byteSource.randomBytes(1)
    actual should be (expected)
  }

  it should "throw an IllegalArgumentException if zero sized array is requested" in {

    intercept[IllegalArgumentException] {
      byteSource.randomBytes(0)
    }
  }

  it should "throw an IllegalArgumentException if negative sized array is requested " in {
    intercept[IllegalArgumentException] {
      byteSource.randomBytes(-1)
    }
  }
}




