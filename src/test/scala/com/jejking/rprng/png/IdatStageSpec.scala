package com.jejking.rprng.png

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, SystemMaterializer}
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import akka.util.{ByteString, ByteStringBuilder}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

/**
  * Created by jking on 23/07/2017.
  */
class IdatStageSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll {

  import IdatStage._

  implicit val system = ActorSystem("test")
  implicit val materializer = SystemMaterializer.get(system)

  "numberOfScanlinesPerIdatChunk" should "compute number nearest in size to 32kb" in {
    val width = 150 // pixels, so scanline will (150 * 4) + 1 bytes = 601

    val expected = 54 // (32 * 1024) / 601f = 54.52246

    numberOfScanlinesPerIdatChunk(width) shouldBe expected
  }

  it should "return 1 if a single scanline would be very nearly 32kb in size" in {
    val width = 8191
    val expected = 1 // (8191 * 4) + 1 = 32765

    numberOfScanlinesPerIdatChunk(width) shouldBe expected
  }

  it should "return 1 if a single scanline would be just over 32kb in size" in {
    val width = 8192
    val expected = 1 // 32k / ((8192f * 4) + 1) = .9999695

    numberOfScanlinesPerIdatChunk(width) shouldBe expected
  }

  "shouldPushDownstream" should "return true if scanlinesPulled equal to scanlinesPerChunk" in {
    shouldPushDownstream(10, 100)(10) shouldBe true
  }

  it should "return true if scanlinesPulled equal to scanlinesPerChunk x 2" in {
    shouldPushDownstream(10, 100)(20) shouldBe true
  }

  it should "return true if scanlinesPulled equal scanlinesPerChunk x 3" in {
    shouldPushDownstream(10, 100)(30) shouldBe true
  }

  it should "return true if scanlinesPulled equal to height" in {
    shouldPushDownstream(10, 100)(100) shouldBe true
    shouldPushDownstream(10, 105)(105) shouldBe true
  }

  it should "return false if scanlinesPulled less than height and not a whole multiple of scanlinesPerChunk" in {
    shouldPushDownstream(10, 100)(1) shouldBe false
    shouldPushDownstream(10, 100)(34) shouldBe false
    shouldPushDownstream(10, 100)(99) shouldBe false
  }

  "the stage" should "emit a single idat chunk and complete the stage for width 100 and height 1" in {

    val scanline = Png.scanline(4, 100)

    val hundredPixels = pixelsForAline(100)

    val scanlineSource = Source.repeat(hundredPixels).map(scanline)
    val idatStage = new IdatStage(100, 1)
    val idat = Png.idat() _
    val expected = idat(scanline(hundredPixels), true)

    scanlineSource
      .via(idatStage)
      .runWith(TestSink.probe[ByteString])
        .request(1)
      .expectNext(expected)
      .expectComplete()
  }

  it should "emit three idat chunks and complete the stage for width 240 * height 100" in {
    val scanline = Png.scanline(4, 240)

    val twoHundredAndFortyPixels = pixelsForAline(240)

    val scanlineSource = Source.repeat(twoHundredAndFortyPixels).map(scanline)
    val idatStage = new IdatStage(240, 100)
    val idat = Png.idat() _

    val targetNumberOfScanlines = IdatStage.numberOfScanlinesPerIdatChunk(240)

    val thirtyFourLines = scanlines(scanline(twoHundredAndFortyPixels), 34)
    val thirtyTwoLines = scanlines(scanline(twoHundredAndFortyPixels), 32)
    scanlineSource
      .via(idatStage)
      .runWith(TestSink.probe[ByteString])
      .request(1)
      .expectNext(idat(thirtyFourLines, false))
      .request(1)
      .expectNext(idat(thirtyFourLines, false))
      .request(1)
      .expectNext(idat(thirtyTwoLines, true))
      .expectComplete()

  }

  override def afterAll(): Unit = {
    system.terminate()
  }

  def pixelsForAline(width: Int) = {
    val onePixel = ByteString(1, 2, 3, 4)
    val byteStringBuilder = new ByteStringBuilder()
    (1 to width).foreach(i => byteStringBuilder ++= onePixel)
    byteStringBuilder.result()
  }

  def scanlines(scanline: ByteString, n: Int): ByteString = {
    val byteStringBuilder = new ByteStringBuilder()
    (1 to n).foreach(i => byteStringBuilder ++= scanline)
    byteStringBuilder.result()
  }

}
