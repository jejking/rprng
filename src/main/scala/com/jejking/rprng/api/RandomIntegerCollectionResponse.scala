package com.jejking.rprng.api

import spray.json.DefaultJsonProtocol

/**
 * Encapsulates response to a request for some random integers.
 */
case class RandomIntegerCollectionResponse(content: Traversable[Traversable[Int]])

object RandomIntegerCollectionResponseProtocol extends DefaultJsonProtocol {
  implicit val format = jsonFormat1(RandomIntegerCollectionResponse.apply)
}
