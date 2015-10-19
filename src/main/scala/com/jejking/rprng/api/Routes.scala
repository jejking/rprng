package com.jejking.rprng.api

import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer


/**
 * Routing for web requests for randomness.
 */
class Routes(streamsHelper: StreamsHelper) {

  val route = byteRoute


  val byteRoute = get {
    pathPrefix("byte") {
      path("block") {
        // extract and validate block size...
        complete {
          streamsHelper.responseForByteBlock(8)
        }
      }
    }
  }

}
