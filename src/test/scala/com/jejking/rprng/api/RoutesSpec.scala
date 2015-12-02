package com.jejking.rprng.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model
import akka.http.scaladsl.model.HttpEntity.{Chunk, Chunked}
import akka.http.scaladsl.model.{ContentTypes, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.ValidationRejection
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.jejking.rprng.rng._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.Future

/**
 * Tests for Routes at HTTP API level.
 */
class RoutesSpec extends FlatSpec with Matchers with ScalaFutures with ScalatestRouteTest with MockFactory with SprayJsonSupport {

  import RandomIntegerCollectionResponseProtocol.format

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


  "/int/list" should "request 1 list of 100 ints between " + Int.MinValue + " and " + Int.MaxValue in {
    val mockStreamsHelper = mock[StreamsHelper]
    (mockStreamsHelper.responseForIntegerCollection _)
      .expects(RandomIntegerCollectionRequest(RandomList))
      .returning(Future.successful(RandomIntegerCollectionResponse(List(1 to 100))))

    val routes = new Routes(mockStreamsHelper)
    Get("/int/list") ~> routes.intRoute ~> check {
      handled shouldBe true
      val resp: RandomIntegerCollectionResponse = responseAs[RandomIntegerCollectionResponse]
      resp.content should have size 1
      resp.content.head should have size 100
    }
  }


  "/int/list?size=10" should "deliver 1 list of 10 ints between " + Int.MinValue + " and " + Int.MaxValue in {
    val mockStreamsHelper = mock[StreamsHelper]
    (mockStreamsHelper.responseForIntegerCollection _)
      .expects(RandomIntegerCollectionRequest(RandomList, size = 10))
      .returning(Future.successful(RandomIntegerCollectionResponse(List(1 to 10))))

    val routes = new Routes(mockStreamsHelper)
    Get("/int/list?size=10") ~> routes.intRoute ~> check {
      handled shouldBe true
      val resp: RandomIntegerCollectionResponse = responseAs[RandomIntegerCollectionResponse]
      resp.content should have size 1
      resp.content.head should have size 10
    }
  }



  "/int/list?size=10&count=2" should "deliver 2 lists of 10 ints between " + Int.MinValue + " and " + Int.MaxValue in {
    val mockStreamsHelper = mock[StreamsHelper]
    (mockStreamsHelper.responseForIntegerCollection _)
      .expects(RandomIntegerCollectionRequest(RandomList, size = 10, count = 2))
      .returning(Future.successful(RandomIntegerCollectionResponse(List(1 to 10, 1 to 10))))

    val routes = new Routes(mockStreamsHelper)
    Get("/int/list?size=10&count=2") ~> routes.intRoute ~> check {
      handled shouldBe true
      val resp: RandomIntegerCollectionResponse = responseAs[RandomIntegerCollectionResponse]
      resp.content should have size 2
      resp.content.foreach(it => it should have size 10)
    }
  }


  "/int/list?min=0&max=1000" should "deliver 1 list of 100 ints between 0 and 100" in {
    val mockStreamsHelper = mock[StreamsHelper]
    (mockStreamsHelper.responseForIntegerCollection _)
      .expects(RandomIntegerCollectionRequest(RandomList, minBound = 0, maxBound = 100))
      .returning(Future.successful(RandomIntegerCollectionResponse(List(1 to 100))))

    val routes = new Routes(mockStreamsHelper)
    Get("/int/list?min=0&max=100") ~> routes.route ~> check {
      handled shouldBe true
      val resp: RandomIntegerCollectionResponse = responseAs[RandomIntegerCollectionResponse]
      resp.content should have size 1
      resp.content.head should have size 100
      resp.content.head.foreach(i => i should (be >= 0 and be <= 100))
    }
  }



 "/int/list?size=10&min=100&max=10" should "result in a 400" in {
   val mockStreamsHelper = mock[StreamsHelper]
   (mockStreamsHelper.responseForIntegerCollection _).expects(*).never()

   val routes = new Routes(mockStreamsHelper)
   Get("/int/list?min=100&max=10") ~> routes.route ~> check {
     response.status shouldBe StatusCodes.BadRequest
   }
 }

 "/int/set" should "give a single set of 100 ints" in {
   val mockStreamsHelper = mock[StreamsHelper]
   (mockStreamsHelper.responseForIntegerCollection _)
     .expects(RandomIntegerCollectionRequest(RandomSet))
     .returning(Future.successful(RandomIntegerCollectionResponse(Set(1 to 100))))

   val routes = new Routes(mockStreamsHelper)
   Get("/int/set") ~> routes.intRoute ~> check {
     handled shouldBe true
     val resp: RandomIntegerCollectionResponse = responseAs[RandomIntegerCollectionResponse]
     resp.content should have size 1
     resp.content.head.toSet should have size 100
   }
 }

 "/int/set?size=100&min=0&max=50" should "result in a 400" in {
   val mockStreamsHelper = mock[StreamsHelper]
   (mockStreamsHelper.responseForIntegerCollection _).expects(*).never()

   val routes = new Routes(mockStreamsHelper)
   Get("/int/set?size=100&min=0&max=50") ~> routes.route ~> check {
     response.status shouldBe StatusCodes.BadRequest
   }
 }


}
