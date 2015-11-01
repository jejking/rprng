package com.jejking.rprng.api

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
  require(minBound < maxBound, "Min bound must be less than max bound")
  if (collectionType == RandomSet) {
    require(maxBound - minBound > size, "Max bound - min bound must be greater than requested set size")
  }
}



