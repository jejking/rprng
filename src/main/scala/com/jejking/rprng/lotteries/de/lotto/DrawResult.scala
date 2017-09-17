package com.jejking.rprng.lotteries.de.lotto

// TODO - lift validation to type level

/**
  * Represents the result of a draw such as in German 6/49.
  *
  * @param numbers a set of 6 integers between 1 and 49 inclusive
  * @param superNumber an integer between 0 and 9 inclusive
  * @throws IllegalArgumentException if input parameters do not meet the above requirements
  */
case class DrawResult(numbers: Set[Integer], superNumber: Integer) {

  require(numbers.size == 6, s"Numbers set must be of size 6. It was of size ${numbers.size}.")
  numbers.foreach((i:Integer) => require(i >= 1 && i <= 49, "Numbers set may only contain numbers between 1 and 49 inclusive."))
  require(superNumber >= 0 && superNumber <= 9, "The superNumber may only be between 0 and 9 inclusive.")
}
