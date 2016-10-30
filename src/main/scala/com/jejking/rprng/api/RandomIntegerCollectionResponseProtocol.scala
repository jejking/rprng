package com.jejking.rprng.api

import spray.json.{DefaultJsonProtocol, RootJsonFormat}

/**
 * Spray Json protocol object to allow response to be marshalled to JSON in the API.
 */
object RandomIntegerCollectionResponseProtocol extends DefaultJsonProtocol {
   implicit val format: RootJsonFormat[RandomIntegerCollectionResponse] = jsonFormat1(RandomIntegerCollectionResponse.apply)
 }
