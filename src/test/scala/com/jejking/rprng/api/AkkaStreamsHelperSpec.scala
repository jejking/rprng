package com.jejking.rprng.api

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import akka.stream.ActorMaterializer
import akka.util.ByteString
import com.jejking.rprng.rng.TestUtils.{FailureActor, InsecureSeeder, ZeroRandomSource}
import com.jejking.rprng.rng._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Millis, Span}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

/**
 * Tests for [[AkkaStreamsHelper]].
 */
class AkkaStreamsHelperSpec extends FlatSpec with Matchers with ScalaFutures with BeforeAndAfterAll {

  implicit val system: ActorSystem = initActorSystem()
  implicit val materializer = ActorMaterializer()
  val akkaStreamsHelper = new AkkaStreamsHelper()


  def initActorSystem(): ActorSystem = {

    val actorSystem = ActorSystem("akkaStreamsHelperSpec")
    actorSystem.actorOf(RandomSourceActor.props(new ZeroRandomSource, new InsecureSeeder), "randomRouter")
    actorSystem.actorOf(Props[FailureActor], "failure")
    actorSystem
  }

  "the akka streams helper" should "deliver a block of random bytes" in {


    whenReady(akkaStreamsHelper.responseForByteBlock(8)) { resp =>

      // correct response code, 200
      resp.status should be (StatusCodes.OK)


      // correct mime type, octet stream
      resp.entity.contentType() should be (ContentTypes.`application/octet-stream`)

      // correct response size, 8 bytes
      resp.entity.getContentLengthOption().asScala should be (Some (8))

      // correct content, 8 zeroes
      whenReady(resp.entity.dataBytes.runFold(ByteString.empty)((acc, bs) => acc ++ bs)) { bs =>

        bs should be (ByteString(0, 0, 0, 0, 0, 0, 0, 0))
      }

    }
  }

  it should "deliver a stream of random bytes in 1kb chunks" in {

    akkaStreamsHelper.responseForByteStream(1024).entity
                      .dataBytes
                      .take(1024)
                      .runForeach(bs => bs shouldBe TestUtils.oneKb)
  }

  it should "deliver a list of ints of size 1 of a list of size 1 when requested" in {
    val req = RandomIntegerCollectionRequest(RandomList, 1, 1, 0, 10)

    whenReady(akkaStreamsHelper.responseForIntegerCollection(req)) { resp =>
      resp.content.size shouldBe 1
      resp.content.foreach(it => it.size shouldBe 1)
      resp.content.foreach(it => it.foreach(i => i shouldBe 0))
    }
  }

  it should "deliver a list of ints of size 100 of a list of size 10 when requested" in {
    val req = RandomIntegerCollectionRequest(RandomList, 100, 10, 0, 10)

    whenReady(akkaStreamsHelper.responseForIntegerCollection(req), timeout(Span(1, Seconds))) { resp =>
      resp.content.size shouldBe 10
      resp.content.foreach(it => it.size shouldBe 100)
      resp.content.foreach(it => it.foreach(i => i shouldBe 0))
    }
  }

  override def afterAll(): Unit = {
    this.system.shutdown()
  }

}
