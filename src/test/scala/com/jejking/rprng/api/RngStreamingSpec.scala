package com.jejking.rprng.api

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.model.{ContentType, ContentTypes, MediaTypes, StatusCodes}
import akka.stream.{ActorMaterializer, SystemMaterializer}
import akka.stream.scaladsl.Sink
import akka.util.{ByteString, ByteStringBuilder}
import com.jejking.rprng.rng.TestUtils.{FailureActor, InsecureSeeder, ZeroRng}
import com.jejking.rprng.rng._
import com.jejking.rprng.rng.actors.RngActor
import org.apache.commons.math3.random.Well44497a
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.SpanSugar._
import org.scalatest.time.{Seconds, Span}

import java.io.ByteArrayInputStream
import java.security.SecureRandom
import javax.imageio.ImageIO
import scala.concurrent.Future


/**
 * Tests for [[AkkaRngStreaming]].
 */
class RngStreamingSpec extends AnyFlatSpec with Matchers with ScalaFutures with BeforeAndAfterAll {

  implicit override val patienceConfig = PatienceConfig(timeout = 2 seconds, interval = 100 milliseconds)

  implicit val system: ActorSystem = initActorSystem()
  implicit val materializer = SystemMaterializer.get(system)
  val simpleAkkaStreamsHelper = new AkkaRngStreaming(system.actorSelection("/user/randomRouter"))

  createProperlyRandomActor()
  val randomAkkaStreamsHelper = new AkkaRngStreaming(system.actorSelection("/user/randomlyRandom"))

  def initActorSystem(): ActorSystem = {

    val actorSystem = ActorSystem("akkaStreamsHelperSpec")
    actorSystem.actorOf(RngActor.props(new ZeroRng, new InsecureSeeder), "randomRouter")
    actorSystem.actorOf(Props[FailureActor](), "failure")
    actorSystem
  }

  def createProperlyRandomActor(): Unit = {
    val secureRandom = new SecureRandom()
    val secureSeeder = new SecureRandomSeeder(secureRandom)
    val randomGenerator = CommonsMathRandomGeneratorFactory.createNewGeneratorInstance[Well44497a]()
    val randomGeneratorByteSource = CommonsMathRng(randomGenerator)
    system.actorOf(RngActor.props(randomGeneratorByteSource, secureSeeder), "randomlyRandom")
  }

  "the akka streams helper" should "deliver a block of random bytes" in {


    whenReady(simpleAkkaStreamsHelper.responseForByteBlock(8)) { resp =>

      // correct response code, 200
      resp.status should be (StatusCodes.OK)


      // correct mime type, octet stream
      resp.entity.contentType should be (ContentTypes.`application/octet-stream`)

      // correct response size, 8 bytes
      resp.entity.getContentLengthOption().getAsLong should be (8)
      // correct content, 8 zeroes
      whenReady(resp.entity.dataBytes.runFold(ByteString.empty)((acc, bs) => acc ++ bs)) { bs =>

        bs should be (ByteString(0, 0, 0, 0, 0, 0, 0, 0))
      }

    }
  }

  it should "deliver a stream of random bytes in 1kb chunks" in {

    simpleAkkaStreamsHelper.responseForByteStream(1024).entity
                      .dataBytes
                      .take(1024)
                      .runForeach(bs => bs shouldBe TestUtils.oneKb)
  }

  it should "deliver a list of ints of size 1 of a list of size 1 when requested" in {
    val req = RandomIntegerCollectionRequest(RandomList, 1, 1, 0, 10)

    whenReady(simpleAkkaStreamsHelper.responseForIntegerCollection(req)) { resp =>
      resp.content.size shouldBe 1
      resp.content.foreach(it => it.size shouldBe 1)
      resp.content.foreach(it => it.foreach(i => i shouldBe 0))
    }
  }

  it should "deliver a list of size 10 of lists of size 100 when requested" in {
    val req = RandomIntegerCollectionRequest(RandomList, 100, 10, 0, 10)

    whenReady(simpleAkkaStreamsHelper.responseForIntegerCollection(req), timeout(Span(1, Seconds))) { resp =>
      resp.content.size shouldBe 10
      resp.content.foreach(it => it.size shouldBe 100)
      resp.content.foreach(it => it.foreach(i => i shouldBe 0))
    }
  }

  it should "deliver a list of size 1 of set of size 10" in {
    val req = RandomIntegerCollectionRequest(RandomSet, 10, 1, 0, 20)

    whenReady(randomAkkaStreamsHelper.responseForIntegerCollection(req)) {
      resp => {
        resp.content should have size 1
        resp.content.foreach(it => {
          it should have size 10
          it.foreach( i => i should (be >= 0 and be <= 20))
        })
      }
    }
  }

  it should "deliver a list of size 10 of sets of size 100" in {

    val req = RandomIntegerCollectionRequest(RandomSet, 100, 10, 0, 2000)
    whenReady(randomAkkaStreamsHelper.responseForIntegerCollection(req)) {
      resp => {
        resp.content should have size 10
        resp.content.foreach(it => {
          it should have size 100
          it.foreach( i => i should (be >= 0 and be <= 2000))
        })
      }
    }
  }

  it should "deliver a png of size 250 * 500" in {

    val width = 250
    val height = 500

    val resp = randomAkkaStreamsHelper.responseForPng(width, height)
    resp.status should be (StatusCodes.OK)
    // correct mime type, png
    resp.entity.contentType should be (ContentType(MediaTypes.`image/png`))
    val body:Future[ByteStringBuilder] = resp.entity.dataBytes
                                        .runWith(Sink.fold(new ByteStringBuilder())((bsb, bs) => bsb ++= bs))
    whenReady(body) {
      body => {
        val byteString = body.result()
        val bufferedImage = ImageIO.read(new ByteArrayInputStream(byteString.toArray))
        bufferedImage.getWidth shouldBe width
        bufferedImage.getHeight shouldBe height
      }
    }


  }

  override def afterAll(): Unit = {
    this.system.terminate()
  }

}
