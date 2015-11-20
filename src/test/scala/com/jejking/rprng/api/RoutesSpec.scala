package com.jejking.rprng.api

import akka.http.scaladsl.model.HttpEntity.{Chunk, Chunked}
import akka.http.scaladsl.model.{ContentTypes, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.ValidationRejection
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.jejking.rprng.rng.TestUtils
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.Future

/**
 * Tests for Routes at HTTP API level.
 */
class RoutesSpec extends FlatSpec with Matchers with ScalaFutures with ScalatestRouteTest with MockFactory {


  val oneKb = TestUtils.byteStringOfZeroes(1024)
  val twoKb = oneKb ++ oneKb

  "/byte/block" should "return 1024 'random' bytes" in {

    val mockStreamsHelper = mock[StreamsHelper]
    (mockStreamsHelper.responseForByteBlock _).expects(1024).returning(Future.successful(HttpResponse(entity = oneKb)))

    val routes = new Routes(mockStreamsHelper)
    Get("/byte/block") ~> routes.byteRoute ~> check {
      responseAs[ByteString] shouldEqual oneKb
    }
  }

  "/byte/block/" should "also return 1024 'random' bytes" in {
    val mockStreamsHelper = mock[StreamsHelper]
    (mockStreamsHelper.responseForByteBlock _).expects(1024).returning(Future.successful(HttpResponse(entity = oneKb)))

    val routes = new Routes(mockStreamsHelper)
    Get("/byte/block/") ~> routes.byteRoute ~> check {
      responseAs[ByteString] shouldEqual oneKb
    }

  }

  "/byte/block/2048" should "return 2048 'random' bytes" in {
    val mockStreamsHelper = mock[StreamsHelper]
    (mockStreamsHelper.responseForByteBlock _).expects(2048).returning(Future.successful(HttpResponse(entity = twoKb)))

    val routes = new Routes(mockStreamsHelper)
    Get("/byte/block/2048") ~> routes.byteRoute ~> check {
      responseAs[ByteString] shouldEqual twoKb
    }

  }

  "/byte/block/0" should "result in a ValidationRejection" in {
    val mockStreamsHelper = mock[StreamsHelper]
    (mockStreamsHelper.responseForByteBlock _).expects(*).never()

    val routes = new Routes(mockStreamsHelper)
    Get("/byte/block/0") ~> routes.byteRoute ~> check {
      rejection shouldBe a [ValidationRejection]
    }
  }

  "/byte/block/forty-two" should "be rejected (not matched)" in {
    val mockStreamsHelper = mock[StreamsHelper]
    (mockStreamsHelper.responseForByteBlock _).expects(*).never()

    val routes = new Routes(mockStreamsHelper)
    Get("/byte/block/forty-two") ~> routes.byteRoute ~> check {
      handled shouldBe false
    }
  }

  "/byte/block/-64" should "be rejected" in {
    val mockStreamsHelper = mock[StreamsHelper]
    (mockStreamsHelper.responseForByteBlock _).expects(*).never()

    val routes = new Routes(mockStreamsHelper)
    Get("/byte/block/-64") ~> routes.byteRoute ~> check {
      handled shouldBe false
    }
  }

  "/byte/block/" + (Integer.MAX_VALUE + 1L) should "be rejected" in {
    val mockStreamsHelper = mock[StreamsHelper]
    (mockStreamsHelper.responseForByteBlock _).expects(*).never()

    val routes = new Routes(mockStreamsHelper)
    Get("/byte/block/" + (Integer.MAX_VALUE + 1L)) ~> routes.byteRoute ~> check {
      handled shouldBe false
    }
  }

  "/byte/stream" should "deliver a chunked response" in {
    val httpResponse = HttpResponse(StatusCodes.OK).withEntity(Chunked(ContentTypes.`application/octet-stream`, Source.single(Chunk(oneKb))))
    val mockStreamsHelper = mock[StreamsHelper]
    (mockStreamsHelper.responseForByteStream _).expects(1024).returning(httpResponse)

    val routes = new Routes(mockStreamsHelper)
    Get("/byte/stream") ~> routes.byteRoute ~> check {
      responseEntity.isChunked() shouldBe true
      whenReady(responseEntity.dataBytes.runFold(ByteString.empty)((acc, in) => acc ++ in)) {
        bs => bs shouldBe oneKb
      }
    }
  }

  /*
  "/int/list" should "deliver 1 list of 100 ints between " + Int.MinValue + " and " + Int.MaxValue in {
    fail("not done")
  }

  "/int/list?size=10" should "deliver 1 list of 10 ints between " + Int.MinValue + " and " + Int.MaxValue in {
    fail("not done")
  }

  "/int/list?size=10&count=2" should "deliver 2 lists of 10 ints between " + Int.MinValue + " and " + Int.MaxValue in {
    fail("not done")
  }

  "/int/list?min=0&max=1000" should "deliver 1 list of 100 ints betweeen 0 and 100" in {
    fail("not done")
  }

  "/int/list?size=10&min=100&max=10" should "be rejected with a 400" in {
    fail("not done")
  }
  */


}
