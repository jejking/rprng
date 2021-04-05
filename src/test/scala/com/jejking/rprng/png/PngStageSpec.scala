package com.jejking.rprng.png

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.SystemMaterializer
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import akka.util.ByteString
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class PngStageSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll {

  implicit val system = ActorSystem("test")
  implicit val materializer = SystemMaterializer.get(system)

  "the png stage" should "stream a png" in {

    val notReallyAnIdat = ByteString(1, 2, 3, 4)
    val iterable: Iterable[ByteString] = Seq(notReallyAnIdat, notReallyAnIdat)
    val dummySource: Source[ByteString, NotUsed] = Source.fromIterator(() => iterable.iterator)
    val pngStage = new PngStage(100, 400)

    dummySource
      .via(pngStage)
      .runWith(TestSink.probe[ByteString])
      .request(1)
      .expectNext(Png.PNG_SIGNATURE)
      .request(1)
      .expectNext(Png.ihdr(100, 400))
      .request(1)
      .expectNext(notReallyAnIdat)
      .request(1)
      .expectNext(notReallyAnIdat)
      .request(1)
      .expectNext(Png.iend())
      .expectComplete()
  }

  override def afterAll(): Unit = {
    system.terminate()
  }

}
