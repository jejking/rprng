package com.jejking.rprng.api

import com.jejking.rprng.rng.RandomIntegerCollectionResponse
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

/**
 * Created by jking on 11/11/15.
 */
object RandomIntegerCollectionResponseProtocol extends DefaultJsonProtocol {
   implicit val format: RootJsonFormat[RandomIntegerCollectionResponse] = jsonFormat1(RandomIntegerCollectionResponse.apply)
 }
