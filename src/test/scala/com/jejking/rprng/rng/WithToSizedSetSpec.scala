package com.jejking.rprng.rng


import org.scalatest.{Matchers, FlatSpec}

/**
 * Tests for [[WithToSizedSet]]
 */
class WithToSizedSetSpec extends FlatSpec with Matchers {

  "the class" should "reject a negative target size" in {
    intercept[IllegalArgumentException] {
      val withToSizedSet = new WithToSizedSet[Int](List.empty)
      withToSizedSet.toSet(-1)
    }
  }

  it should "return an empty set with request for zero target size" in {
    val withToSizedSet = new WithToSizedSet[Int](1 to 5)
    withToSizedSet.toSet(0) shouldBe Set.empty
  }

  it should "return an empty set with request for target size on an empty traversable" in {
    val withToSizedSet = new WithToSizedSet[Int](List.empty)
    withToSizedSet.toSet(10) shouldBe Set.empty
  }

  it should "return a set of size 4 with request for target size 6 on a four int sequence" in {
    val withToSizedSet = new WithToSizedSet[Int](1 to 4)
    withToSizedSet.toSet(6) shouldBe (1 to 4).toSet

  }

  it should "return a set of size 6 with request for target size 6 on a six int sequence" in {
    val withToSizedSet = new WithToSizedSet[Int](1 to 6)
    withToSizedSet.toSet(6) shouldBe (1 to 6).toSet
  }

  it should "return a set of size 6 with request for target size 6 on a 10 int sequence" in {
    val withToSizedSet = new WithToSizedSet[Int](1 to 10)
    withToSizedSet.toSet(6) shouldBe (1 to 6).toSet
  }

}
