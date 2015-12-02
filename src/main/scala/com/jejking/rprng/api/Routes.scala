package com.jejking.rprng.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route, ValidationRejection}
import com.jejking.rprng.rng.{RandomSet, RandomList, RandomIntegerCollectionRequest}

/**
 * Routing for web requests for randomness.
 */
class Routes(streamsHelper: StreamsHelper) extends SprayJsonSupport {

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
      parameters('size.as[Int] ? 100, 'count.as[Int]? 1, 'min.as[Int] ? Int.MinValue, 'max.as[Int] ? Int.MaxValue) {
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

  val route = handleExceptions(theExceptionHandler) {
    byteRoute ~ intRoute
  }


}
