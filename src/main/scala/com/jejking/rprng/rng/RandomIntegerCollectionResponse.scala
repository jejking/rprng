package com.jejking.rprng.rng

import spray.json.{DefaultJsonProtocol, RootJsonFormat}

/**
 * Encapsulates response to a request for some random integers.
 */
case class RandomIntegerCollectionResponse(content: Iterable[Iterable[Int]])


