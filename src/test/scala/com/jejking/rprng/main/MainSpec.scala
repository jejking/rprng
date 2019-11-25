package com.jejking.rprng.main

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpRequest, StatusCodes}
import akka.stream.ActorMaterializer
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.SpanSugar._
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import spray.json._

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, FiniteDuration}

/**
  * Tests that the server starts and the API is exposed
  * as expected.
  */
class MainSpec extends FlatSpec with Matchers with ScalaFutures with BeforeAndAfterAll {

  implicit override val patienceConfig = PatienceConfig(timeout = 15 seconds, interval = 250 milliseconds)


  val baseUri = "http://localhost:8080"

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  override def beforeAll() {
    // start and wait for the binding to be ready before trying to use it
    val myConfig = processConfig(ConfigFactory.load())
    Await.ready(Main.createAndStartServer(myConfig), (Duration(5, "seconds")))
  }

  "/byte/block" should "deliver 1kb of bytes" in {

    val futureResp = Http().singleRequest(HttpRequest(uri = s"$baseUri/byte/block"))
    whenReady(futureResp) { resp =>
      // correct response code, 200
      resp.status should be (StatusCodes.OK)


      // correct mime type, octet stream
      resp.entity.contentType should be (ContentTypes.`application/octet-stream`)

      // correct response size, 1024 bytes
      resp.entity.getContentLengthOption().getAsLong should be (1024)

    }
  }

  it should "allow the block size to be specified" in {
    val futureResp = Http().singleRequest(HttpRequest(uri = s"$baseUri/byte/block/512"))
    whenReady(futureResp) { resp =>
      // correct response code, 200
      resp.status should be (StatusCodes.OK)


      // correct mime type, octet stream
      resp.entity.contentType should be (ContentTypes.`application/octet-stream`)

      // correct response size, 512 bytes
      resp.entity.getContentLengthOption().getAsLong should be (512)

    }
  }

  "/byte/stream" should "deliver a stream of bytes until the connection is stopped" in {
    val futureResp = Http().singleRequest(HttpRequest(uri = s"$baseUri/byte/stream"))
    whenReady(futureResp) { resp =>
      // correct response code, 200
      resp.status should be(StatusCodes.OK)


      // correct mime type, octet stream
      resp.entity.contentType should be(ContentTypes.`application/octet-stream`)

      // no length given as indeterminate
      resp.entity.getContentLengthOption().isPresent should be(false)

      resp.entity.dataBytes.takeWithin(FiniteDuration(250, TimeUnit.MILLISECONDS)).runForeach(bs => {})

    }
  }

  "/int/list" should "deliver a json object where 'content' is a single list of 100 ints" in {
    val futureResp = Http().singleRequest(HttpRequest(uri = s"$baseUri/int/list"))
    whenReady(futureResp) { resp =>
      // correct response code, 200
      resp.status should be (StatusCodes.OK)

      // correct mime type, json
      resp.entity.contentType should be (ContentTypes.`application/json`)

      whenReady(resp.entity.dataBytes.runFold(ByteString.empty)((acc, bs) => acc ++ bs)) { bs =>

        val json = new String(bs.toArray).parseJson.asJsObject

        val contentFields = json.getFields("content")
        contentFields.size should be (1)

        val list = contentFields.head.asInstanceOf[JsArray]
          .elements
            .head.asInstanceOf[JsArray].elements

        list.size should be (100)

        list.map((jsvalue) => jsvalue.asInstanceOf[JsNumber])
              .map(jsnumber => jsnumber.value)
              .foreach((bigdecimal) => {
                bigdecimal.isValidInt should be (true)
              })
      }

    }

  }

  it should "allow size, count, min and max to be configurable" in {
    val futureResp = Http().singleRequest(HttpRequest(uri = s"$baseUri/int/list?size=10&count=10&min=0&max=100"))
    whenReady(futureResp) { resp =>
      // correct response code, 200
      resp.status should be (StatusCodes.OK)

      // correct mime type, json
      resp.entity.contentType should be (ContentTypes.`application/json`)

      whenReady(resp.entity.dataBytes.runFold(ByteString.empty)((acc, bs) => acc ++ bs)) { bs =>

        val json = new String(bs.toArray).parseJson.asJsObject

        val contentFields = json.getFields("content")
        contentFields.size should be (1)

        val collections = contentFields.head.asInstanceOf[JsArray]
        collections.elements.size should be (10)

        collections.elements.foreach((jsvalue) => {

          val innerCol = jsvalue.asInstanceOf[JsArray]
          innerCol.elements.size should be (10)
          innerCol.elements.map((jsvalue) => jsvalue.asInstanceOf[JsNumber])
            .map(jsnumber => jsnumber.value)
            .foreach((bigdecimal) => {
              bigdecimal.isValidInt should be (true)
              val fieldVal = bigdecimal.intValue
              fieldVal should be >= 0
              fieldVal should be <= 100
            })
        })

      }

    }
  }

  "/int/set" should "deliver a json object where 'content' is a single set of 100 ints" in {
    val futureResp = Http().singleRequest(HttpRequest(uri = s"$baseUri/int/list"))
    whenReady(futureResp) { resp =>
      // correct response code, 200
      resp.status should be(StatusCodes.OK)

      // correct mime type, json
      resp.entity.contentType should be(ContentTypes.`application/json`)

      whenReady(resp.entity.dataBytes.runFold(ByteString.empty)((acc, bs) => acc ++ bs)) { bs =>

        val json = new String(bs.toArray).parseJson.asJsObject

        val contentFields = json.getFields("content")
        contentFields.size should be(1)

        val list = contentFields.head.asInstanceOf[JsArray]
          .elements
          .head.asInstanceOf[JsArray].elements

        list.size should be(100)

        list.map((jsValue) => jsValue.asInstanceOf[JsNumber])
          .map(jsnumber => jsnumber.value.intValue)
          .toSet.size should be(100)
      }

    }
  }



  override def afterAll() {
    Main.shutdown()
    system.terminate()
  }


}
