package com.jejking.rprng.api

import akka.event.Logging
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route, ValidationRejection}

/**
 * Routing for web requests for randomness.
 */
class Routes(streamsHelper: RngStreaming) extends SprayJsonSupport {

  implicit val theRejectionHandler = RejectionHandler.newBuilder()
                                      .handle {
                                        case ValidationRejection(reason, cause) => {
                                          complete(HttpResponse(StatusCodes.BadRequest, entity = reason))
                                        }
                                      }.result()

   val theExceptionHandler: ExceptionHandler =
    ExceptionHandler {
      case e: IllegalArgumentException => complete(HttpResponse(StatusCodes.BadRequest, entity = e.getMessage))
    }



  implicit val randomIntCollectionFormat = RandomIntegerCollectionResponseProtocol.format


  val byteRoute: Route = get {
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

  val intRoute: Route = get {
    pathPrefix("int") {
      parameters("size".as[Int] ? 100, "count".as[Int]? 1, "min".as[Int] ? Int.MinValue, "max".as[Int] ? Int.MaxValue) {
        (size: Int, count: Int, min: Int, max: Int) => {
          pathPrefix("list") {
            complete {
              val req = RandomIntegerCollectionRequest(RandomList, size, count, min, max)
              streamsHelper.responseForIntegerCollection(req)
            }
          } ~
          pathPrefix("set") {
            complete {
              val req = RandomIntegerCollectionRequest(RandomSet, size, count, min, max)
              streamsHelper.responseForIntegerCollection(req)
            }
          }

        }
      }
    }
  }

  val pngRoute: Route = get {
    pathPrefix("png") {
      parameters("width".as[Int] ? 250, "height".as[Int] ? 250) {
        (width: Int, height: Int) => {
          validate(width > 0 && height > 0, "Width and height must both be greater than 0") {
            complete {
              streamsHelper.responseForPng(width, height)
            }
          }
        }
      }
    }
  }


  val route = handleExceptions(theExceptionHandler) {
    logRequest("rprng-routes",  Logging.InfoLevel) {
      byteRoute ~ intRoute ~ pngRoute
    }
  }


}
