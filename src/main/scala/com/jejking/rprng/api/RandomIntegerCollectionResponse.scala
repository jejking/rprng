package com.jejking.rprng.api

import spray.json.{RootJsonFormat, DefaultJsonProtocol}

/**
 * Encapsulates response to a request for some random integers.
 */
case class RandomIntegerCollectionResponse(content: Iterable[Iterable[Int]])

object RandomIntegerCollectionResponseProtocol extends DefaultJsonProtocol {
  implicit val format: RootJsonFormat[RandomIntegerCollectionResponse] = jsonFormat1(RandomIntegerCollectionResponse.apply)
}
