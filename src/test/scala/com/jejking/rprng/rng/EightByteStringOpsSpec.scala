package com.jejking.rprng.rng

import akka.util.ByteString
import org.apache.commons.math3.random.MersenneTwister
import org.apache.commons.math3.stat.Frequency
import org.scalatest.{FlatSpec, Matchers}

/**
  * Tests for [[EightByteStringOps]].
  */
class EightByteStringOpsSpec extends FlatSpec with Matchers  {

  import EightByteStringOps._

  object EightByteStringGenerator {

    val randomGenerator = new MersenneTwister()

    def generateEightByteString(): EightByteString = {
      val eightBytes = Array[Byte](0,0,0,0,0,0,0,0)
      randomGenerator.nextBytes(eightBytes)
      EightByteString(ByteString(eightBytes))
    }
  }

  val zeroes = EightByteString(ByteString(0,0,0,0,0,0,0,0))
  val ones = EightByteString(ByteString(1,1,1,1,1,1,1,1))
  val oneTwoSevens = EightByteString(ByteString(127,127,127,127,127,127,127,127))

  "toInt with no bounds" should "convert an EightByteString to an Int" in {
    toInt(zeroes) shouldBe makeInt(0,0,0,0)
    toInt(ones) shouldBe makeInt(1, 1, 1, 1)
    toInt(oneTwoSevens) shouldBe makeInt(127, 127, 127, 127)
  }

  "toLong" should "convert an EightByteString to a Long" in {
    toLong(zeroes) shouldBe makeLong(0,0,0,0,0,0,0,0)
    toLong(ones) shouldBe makeLong(1, 1, 1, 1, 1, 1, 1, 1)
    toLong(oneTwoSevens) shouldBe makeLong(127, 127, 127, 127, 127, 127, 127, 127)
  }

  "toDouble" should "convert an EightByteString to a Double" in {
    toDouble(zeroes) shouldBe java.lang.Double.longBitsToDouble(makeLong(0,0,0,0,0,0,0,0))
    toDouble(ones) shouldBe java.lang.Double.longBitsToDouble(makeLong(1, 1, 1, 1, 1, 1, 1, 1))
    toDouble(oneTwoSevens) shouldBe java.lang.Double.longBitsToDouble(makeLong(127, 127, 127, 127, 127, 127, 127, 127))
  }

  "toDoubleBetweenZeroAndOne" should "convert to a double between zero and one" in {

    for (i <- 1 to 10000) {
      val eightBytes = EightByteStringGenerator.generateEightByteString()
      val theDouble = toDoubleBetweenZeroAndOne(eightBytes)
      assert(theDouble >= 0)
      assert(theDouble < 1)
    }

  }

  "toInt with upperBound only" should "reject an upper bound of zero" in {
    intercept[IllegalArgumentException] {
      toInt(zeroes, 0)
    }
  }

  it should "reject a negative upper bound" in {
    intercept[IllegalArgumentException] {
      toInt(zeroes, -1)
    }
  }

  it should "scale down an integer with approximate uniform distribution" in {
    val frequency = new Frequency()
    for (i <- 1 to 10000) {
      val eightBytes = EightByteStringGenerator.generateEightByteString()
      val randomInt = toInt(eightBytes, 10)
      randomInt >= 0 should be (true)
      randomInt < 10 should be (true)
      frequency.addValue(randomInt)
    }
    // very rough check for proper distribution
    for (i <- 0 to 9) {
      frequency.getPct(i) should be (0.1 +- 0.01)
    }
  }

  "toInt with lower and upper bound" should "reject lower bound greater than upper bound" in {
    intercept[IllegalArgumentException] {
      toInt(zeroes, 3, 2)
    }
  }

  it should "reject lower bound equal to upper bound" in {
    intercept[IllegalArgumentException] {
      toInt(zeroes, 3, 3)
    }
  }

  it should "respect the bounds between min and max when requesting a positive range" in {
    val frequency = new Frequency()
    for (i <- 1 to 10000) {
      val eightBytes = EightByteStringGenerator.generateEightByteString()
      val randomInt = toInt(eightBytes, 10, 20)
      randomInt  should be >= 10
      randomInt should be < 20
      frequency.addValue(randomInt)
    }
    // rough check for proper distribution
    for (i <- 10 to 19) {
      frequency.getPct(i) should be (0.1 +- 0.01)
    }
  }

  it should "respect the bounds between min and max when using negative to positive range" in {

    val frequency = new Frequency()
    for (i <- 1 to 10000) {
      val eightBytes = EightByteStringGenerator.generateEightByteString()
      val randomInt = toInt(eightBytes, -10, 20)
      randomInt should be >= -10
      randomInt should be < 20
      frequency.addValue(randomInt)
    }

    // rough check for proper distribution
    for (i <- -10 to 19) {
      frequency.getPct(i) should be (0.03 +- 0.015)
    }

  }

  it should "respect the bounds between min and max when using range of negative integers" in {

    val frequency = new Frequency()
    for (i <- 1 to 10000) {
      val eightBytes = EightByteStringGenerator.generateEightByteString()
      val randomInt = toInt(eightBytes, -20, -10)
      randomInt should be >= -20
      randomInt should be < -10
      frequency.addValue(randomInt)
    }

    // rough check for proper distribution
    for (i <- -20 to -11) {
      frequency.getPct(i) should be (0.1 +- 0.01)
    }
  }


  // from the depths of the JDK, the Bits class
  private def makeInt(b3: Byte, b2: Byte, b1: Byte, b0: Byte): Int =  {
    ((b3) << 24) | ((b2 & 0xff) << 16) | ((b1 & 0xff) << 8) | b0 & 0xff
  }

  private def makeLong(b7: Byte, b6: Byte, b5: Byte, b4: Byte, b3: Byte, b2: Byte, b1: Byte, b0: Byte): Long = {
    ((b7.toLong) << 56)        |
    ((b6.toLong & 0xff) << 48) |
    ((b5.toLong & 0xff) << 40) |
    ((b4.toLong & 0xff) << 32) |
    ((b3.toLong & 0xff) << 24) |
    ((b2.toLong & 0xff) << 16) |
    ((b1.toLong & 0xff) << 8)  |
    b0.toLong & 0xff
  }
}
