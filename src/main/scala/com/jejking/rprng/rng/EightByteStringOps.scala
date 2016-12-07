package com.jejking.rprng.rng

/**
  * Functions to operate on an [[EightByteString]] with a view
  * to supporting RNG operations.
  */
object EightByteStringOps {

  private val DOUBLE_UNIT: Double =  1.0 / (1L << 53)

  /**
    * Converts to an [[Int]] - effectively takes the first four bytes
    * and converts these to the expected signed 32 bit integer.
    * @param eightByteString input
    * @return converted int
    */
  def toInt(eightByteString: EightByteString): Int = {
    eightByteString.byteString.toByteBuffer.asIntBuffer().get()
  }

  /**
    * Converts to an [[Int]], scaling down to a value between zero (inclusive)
    * and the upper bound (exclusive).
    * @param eightByteString input
    * @param upperBound exclusive upper bound
    * @return converted int
    */
  def toInt(eightByteString: EightByteString, upperBound: Int): Int = {
    require(upperBound > 0, s"Bound must be strictly positive, but was $upperBound")

    val result: Int = (toDoubleBetweenZeroAndOne(eightByteString) * upperBound).toInt
    if (result < upperBound) {
      result
    } else {
      result - 1
    }
  }

  /**
    * Converts to an int, scaled down to be between the lower bound (inclusive)
    * and the upper bound (exclusive). In the edge case that lower bound is [[Int.MinValue]]
    * and the upper bound is [[Int.MaxValue]] then the behaviour is the same as [[EightByteStringOps.toInt()]]
    * with no parameters.
    *
    * @param eightByteString input
    * @param lowerBound inclusive lower bound
    * @param upperBound exclusive upper bound
    * @return converted int
    */
  def toInt(eightByteString: EightByteString, lowerBound: Int, upperBound: Int): Int = {
    require(lowerBound < upperBound, s"lowerBound (${lowerBound} must be less than upperBound ${upperBound}")

    if (lowerBound == Int.MinValue && upperBound == Int.MaxValue) {
      toInt(eightByteString)
    } else {
      toInt(eightByteString, (upperBound - lowerBound).abs) + lowerBound
    }
  }

  /**
    * Converts to a [[Double]] using the full eight bytes.
    * @param eightByteString input
    * @return converted double
    */
  def toDouble(eightByteString: EightByteString): Double = {
    eightByteString.byteString.toByteBuffer.asDoubleBuffer().get()
  }

  /**
    * Converts to a [[Double]], scaled down to be between zero and one.
    * @param eightByteString input
    * @return scaled down double
    */
  def toDoubleBetweenZeroAndOne(eightByteString: EightByteString): Double = {
    /*
     Note that the left most 53 bits of a 64 bit double are the fractional part.
     So bit-shifting 11 to the left removes the exponent and we can then multiply
     by the DOUBLE_UNIT constant which ensures the resulting double is between 0 and 1.
     */

    (toLong(eightByteString) >>> 11) * DOUBLE_UNIT
  }

  /**
    * Converst to a [[Long]] using the full eight bytes.
    * @param eightByteString input
    * @return converted to a long
    */
  def toLong(eightByteString: EightByteString): Long = {
    eightByteString.byteString.toByteBuffer.asLongBuffer().get()
  }

}
