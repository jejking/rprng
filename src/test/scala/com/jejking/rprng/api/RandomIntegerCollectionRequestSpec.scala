package com.jejking.rprng.api

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

/**
 * Tests for [[RandomIntegerCollectionRequest]].
 */
class RandomIntegerCollectionRequestSpec extends AnyFlatSpec with Matchers {

  "a request" should "be constructed OK given valid input" in {
    RandomIntegerCollectionRequest(RandomList)
    RandomIntegerCollectionRequest(RandomList, size = 20, count = 2, minBound = 10, maxBound = 2000)
  }

  it should "accept a request for a set from the full range of integers" in {
    RandomIntegerCollectionRequest(RandomSet)
  }

  it should "accept a request for a set from nearly the full range of integers" in {
    RandomIntegerCollectionRequest(RandomSet, maxBound = Int.MaxValue - 1)
  }

  it should "accept a request for a set from constrained range of positive integers" in {
    RandomIntegerCollectionRequest(RandomSet, size = 5, minBound = 10, maxBound = 20)
  }

  it should "accept a request for a set from bounds between zero and 10" in {
    RandomIntegerCollectionRequest(RandomSet, size = 5, minBound = 0, maxBound = 10)
  }

  it should "accept a request for a set from bounds between -10 and zero" in {
    RandomIntegerCollectionRequest(RandomSet, size = 5, minBound = -10, maxBound = 0)
  }

  it should "accept a request for a set from bounds between -10 and =10" in {
    RandomIntegerCollectionRequest(RandomSet, size = 10, minBound = -10, maxBound = 10)
  }


  it should "reject request for an empty list" in {
    intercept[IllegalArgumentException] {
      RandomIntegerCollectionRequest(RandomList, size = 0, count = 2)
    }
  }

  it should "reject a request for negatively sized list" in {
    intercept[IllegalArgumentException] {
      RandomIntegerCollectionRequest(RandomList, size = -1, count = 2)
    }
  }

  it should "reject a request for zero lists" in {
    intercept[IllegalArgumentException] {
      RandomIntegerCollectionRequest(RandomList, count = 0)
    }
  }

  it should "reject a request for a negative number of lists" in {
    intercept[IllegalArgumentException] {
      RandomIntegerCollectionRequest(RandomList, count = -1)
    }
  }

  it should "reject a request where the minBound is equal to the maxBound" in {
    intercept[IllegalArgumentException] {
      RandomIntegerCollectionRequest(RandomList, minBound = 100, maxBound = 100)
    }
  }

  it should "reject a request where the minBound is greater than the maxBound" in {
    intercept[IllegalArgumentException] {
      RandomIntegerCollectionRequest(RandomList, minBound = 100, maxBound = 50)
    }
  }

  it should "reject a request where for a set where the span between min and max bound is equal to the requested size" in {
    intercept[IllegalArgumentException] {
      RandomIntegerCollectionRequest(RandomSet, size = 10, minBound = 0, maxBound = 10)
    }
  }

  it should "reject a request where for a set where the span between min and max bound is less than the requested size" in {
    intercept[IllegalArgumentException] {
      RandomIntegerCollectionRequest(RandomSet, size = 10, minBound = 0, maxBound = 9)
    }
  }

}
