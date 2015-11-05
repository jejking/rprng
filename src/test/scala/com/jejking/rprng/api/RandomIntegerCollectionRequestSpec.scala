package com.jejking.rprng.api

import org.scalatest.{Matchers, FlatSpec}

/**
 * Tests for [[RandomIntegerCollectionRequest]].
 */
class RandomIntegerCollectionRequestSpec extends FlatSpec with Matchers {

  /**
   * require(size > 0, "Size must be strictly positive")
  require(count > 0, "Count must be strictly positive")
  require(minBound < maxBound, "Min bound must be less than max bound")
  if (collectionType == RandomSet) {
    require(maxBound - minBound > size, "Max bound - min bound must be greater than requested set size")
  }
   */

  "a request" should "be constructed OK given valid input" in {
    RandomIntegerCollectionRequest(RandomList)
    RandomIntegerCollectionRequest(RandomList, size = 20, count = 2, minBound = 10, maxBound = 2000)
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
