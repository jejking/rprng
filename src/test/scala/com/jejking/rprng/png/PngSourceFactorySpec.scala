package com.jejking.rprng.png

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import akka.util.ByteStringBuilder
import com.jejking.rprng.main.{RprngConfig, createRandomSourceActors}
import com.jejking.rprng.rng.actors.TimeRangeToReseed
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

class PngSourceFactorySpec extends FlatSpec with ScalaFutures with Matchers with BeforeAndAfterAll {

  implicit override val patienceConfig = PatienceConfig(timeout = 1 second, interval = 100 milliseconds)

  implicit val system = ActorSystem("test")
  implicit val materializer = ActorMaterializer()
  val routerActorRef = createRandomSourceActors(system, RprngConfig(0, TimeRangeToReseed(1 hour, 8 hours), 8))

  "the png source factory" should "generate a stream that can be parsed into a proper PNG by Java ImageIO" in {
    val pngSourceFactory = PngSourceFactory.pngSource(system.actorSelection(routerActorRef.path)) _
    val width = 100
    val height = 400
    val res:Future[ByteStringBuilder] = pngSourceFactory(width, height)
                                          .runWith(Sink.fold(new ByteStringBuilder())((bsb, bs) => bsb ++= bs))

    whenReady(res) { res =>
      val byteString = res.result()
      val bufferedImage = ImageIO.read(new ByteArrayInputStream(byteString.toArray))
      bufferedImage.getWidth shouldBe width
      bufferedImage.getHeight shouldBe height
    }
  }

  override def afterAll(): Unit = {
    system.terminate()
  }

}
