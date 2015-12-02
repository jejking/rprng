package com.jejking.rprng.rng

/**
 * Encapsulates response to a request for some random integers.
 */
case class RandomIntegerCollectionResponse(content: Iterable[Iterable[Int]])


