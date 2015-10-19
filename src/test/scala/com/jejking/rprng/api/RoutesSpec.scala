package com.jejking.rprng.api

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.scaladsl.RunnableGraph
import akka.util.ByteString
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, FlatSpec}

import scala.concurrent.Future

/**
 * Tests for Routes at HTTP API level.
 */
class RoutesSpec extends FlatSpec with Matchers with ScalatestRouteTest with MockFactory {



  "/byte/block" should "return 8 'random' bytes" in {

    val oneToEight = ByteString(Array[Byte](1, 2, 3, 4, 5, 6, 7, 8))
    val mockStreamsHelper = mock[StreamsHelper]
    (mockStreamsHelper.responseForByteBlock _).expects(8).returning(Future.successful(HttpResponse(entity = oneToEight)))

    val routes = new Routes(mockStreamsHelper)
    Get("/byte/block") ~> routes.byteRoute ~> check {
      responseAs[ByteString] shouldEqual oneToEight
    }
  }

}
