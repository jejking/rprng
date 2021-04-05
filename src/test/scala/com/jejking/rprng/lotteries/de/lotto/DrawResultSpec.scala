package com.jejking.rprng.lotteries.de.lotto

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers


class DrawResultSpec extends AnyFlatSpec with Matchers {

  "the draw result validation" should "permit a correct result to be created" in {
    val drawResult = DrawResult(Set(1, 2, 3, 4, 5, 6), 7)
  }

  it should "report an error if number set is of size 5" in {
    the [IllegalArgumentException] thrownBy {
      val drawResult = DrawResult(Set(1, 2, 3, 4, 5), 7)
    } should have message "requirement failed: Numbers set must be of size 6. It was of size 5."
  }

  it should "report an error if number set is of size 7" in {
    the [IllegalArgumentException] thrownBy {
      val drawResult = DrawResult(Set(1, 2, 3, 4, 5, 6, 7), 7)
    } should have message "requirement failed: Numbers set must be of size 6. It was of size 7."
  }

  it should "report an error if number set contains a number less than 1" in {
    the [IllegalArgumentException] thrownBy {
      val drawResult = DrawResult(Set(0, 2, 3, 4, 5, 6), 7)
    } should have message "requirement failed: Numbers set may only contain numbers between 1 and 49 inclusive."
  }

  it should "report an error if number set contains a number greater than 49" in {
    the [IllegalArgumentException] thrownBy {
      val drawResult = DrawResult(Set(1, 2, 3, 4, 5, 50), 7)
    } should have message "requirement failed: Numbers set may only contain numbers between 1 and 49 inclusive."
  }

  it should "report an error if superNumber is less than 0" in {
    the [IllegalArgumentException] thrownBy {
      val drawResult = DrawResult(Set(1, 2, 3, 4, 5, 6), -1)
    } should have message "requirement failed: The superNumber may only be between 0 and 9 inclusive."
  }

  it should "report an error if superNumber is greater than 9" in {
    the [IllegalArgumentException] thrownBy {
      val drawResult = DrawResult(Set(1, 2, 3, 4, 5, 6), 10)
    } should have message "requirement failed: The superNumber may only be between 0 and 9 inclusive."
  }



}
