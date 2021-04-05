package com.jejking.rprng.rng

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers


/**
 * Tests for [[WithToSizedSet]]
 */
class WithToSizedSetSpec extends AnyFlatSpec with Matchers {

  "the class" should "reject a negative target size" in {
    intercept[IllegalArgumentException] {
      val withToSizedSet = new WithToSizedSet[Int](List.empty)
      withToSizedSet.toSizedSet(-1)
    }
  }

  it should "return an empty set with request for zero target size" in {
    val withToSizedSet = new WithToSizedSet[Int](1 to 5)
    withToSizedSet.toSizedSet(0) shouldBe Set.empty
  }

  it should "return an empty set with request for target size on an empty traversable" in {
    val withToSizedSet = new WithToSizedSet[Int](List.empty)
    withToSizedSet.toSizedSet(10) shouldBe Set.empty
  }

  it should "return a set of size 4 with request for target size 6 on a four int sequence" in {
    val withToSizedSet = new WithToSizedSet[Int](1 to 4)
    withToSizedSet.toSizedSet(6) shouldBe (1 to 4).toSet

  }

  it should "return a set of size 6 with request for target size 6 on a six int sequence" in {
    val withToSizedSet = new WithToSizedSet[Int](1 to 6)
    withToSizedSet.toSizedSet(6) shouldBe (1 to 6).toSet
  }

  it should "return a set of size 6 with request for target size 6 on a 10 int sequence" in {
    val withToSizedSet = new WithToSizedSet[Int](1 to 10)
    withToSizedSet.toSizedSet(6) shouldBe (1 to 6).toSet
  }

}
