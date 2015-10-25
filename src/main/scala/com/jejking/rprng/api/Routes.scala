package com.jejking.rprng.api

import akka.http.scaladsl.model.{StatusCodes, HttpResponse}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ValidationRejection, RejectionHandler}
import akka.stream.ActorMaterializer


/**
 * Routing for web requests for randomness.
 */
class Routes(streamsHelper: StreamsHelper) {

  implicit val theRejectionHandler = RejectionHandler.newBuilder()
                                      .handle {
                                        case ValidationRejection(reason, cause) => {
                                          complete(HttpResponse(StatusCodes.BadRequest, entity = reason))
                                        }
                                      }.result()


  val byteRoute = get {
    pathPrefix("byte") {
      pathPrefix("block") {
        pathEndOrSingleSlash {
          complete {
            streamsHelper.responseForByteBlock(1024)
          }
        } ~
        path(IntNumber) {
          requestedBytes => {
            validate(requestedBytes > 0, "Must request at least one byte") {
              complete {
                streamsHelper.responseForByteBlock(requestedBytes)
              }
            }
          }
        }
      } ~
      pathPrefix("stream") {
        complete {
          streamsHelper.responseForByteStream(1024)
        }
      }
    }
  }

  val route = byteRoute

}
