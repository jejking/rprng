package com.jejking.rprng.rng

/**
  * Functions to operate on an [[EightByteString]] with a view
  * to supporting RNG operations.
  */
object EightByteStringOps {

  val DOUBLE_UNIT: Double =  1.0 / (1L << 53)

  def toInt(eightByteString: EightByteString): Int = {
    eightByteString.byteString.toByteBuffer.asIntBuffer().get()
  }

  def toInt(eightByteString: EightByteString, upperBound: Int): Int = {
    require(upperBound > 0, s"Bound must be strictly positive, but was $upperBound")
    val result: Int = (toDoubleBetweenZeroAndOne(eightByteString) * upperBound).toInt
    if (result < upperBound) {
      result
    } else {
      result - 1
    }
  }

  def toInt(eightByteString: EightByteString, lowerBound: Int, upperBound: Int): Int = {
    require(lowerBound < upperBound, s"lowerBound (${lowerBound} must be less than upperBound ${upperBound}")

    if (lowerBound == Int.MinValue && upperBound == Int.MaxValue) {
      toInt(eightByteString)
    } else {
      toInt(eightByteString, (upperBound - lowerBound).abs) + lowerBound
    }
  }

  def toDouble(eightByteString: EightByteString): Double = {
    eightByteString.byteString.toByteBuffer.asDoubleBuffer().get()
  }

  def toDoubleBetweenZeroAndOne(eightByteString: EightByteString): Double = {
    (toLong(eightByteString) >>> 11) * DOUBLE_UNIT
  }

  def toLong(eightByteString: EightByteString): Long = {
    eightByteString.byteString.toByteBuffer.asLongBuffer().get()
  }

}
