package com.jejking.rprng.api

import com.jejking.rprng.rng.RandomIntRequest

sealed trait RandomCollectionType
case object RandomList extends RandomCollectionType
case object RandomSet extends RandomCollectionType

/**
 * Request for collection(s) of integers.
 * @param collectionType either a list or a set. If a set is requested, then the difference between the min and max
 *                       bound must be larger than the requested set size as, obviously, otherwise the set cannot
 *                       possibly be filled.
 * @param size the size of each collection. Must be strictly positive, i.e. 1 or more.
 * @param count the number of collections (each of the requested size). Must be strictly positive, i.e. 1 or more
 * @param minBound inclusive minimum bound. Must be smaller than the maxBound
 * @param maxBound inclusive maximum bound. Must be greater than minBound.
 * @throws IllegalArgumentException if the described preconditions are not met.
 */
case class RandomIntegerCollectionRequest(collectionType: RandomCollectionType, size: Int = 100, count: Int = 1,
                                          minBound: Int = Integer.MIN_VALUE, maxBound: Int = Integer.MAX_VALUE) {
  require(size > 0, "Size must be strictly positive")
  require(count > 0, "Count must be strictly positive")
  require(minBound < maxBound, s"Min bound ($minBound) must be less than max bound ($maxBound)")
  if (collectionType == RandomSet) {
    // accept defaults without doing the math
    if (minBound != Integer.MIN_VALUE && maxBound != Integer.MAX_VALUE) {
      val range: Long = maxBound.toLong - minBound.toLong
      require(range > size, "Range specified must be greater than requested set size")
    }
  }

  /**
   * Extracts [[minBound]] and [[maxBound]] as a [[RandomIntRequest]]
   * @return corresponding object
   */
  def randomIntRequest(): RandomIntRequest = RandomIntRequest(this.minBound, this.maxBound)
}
