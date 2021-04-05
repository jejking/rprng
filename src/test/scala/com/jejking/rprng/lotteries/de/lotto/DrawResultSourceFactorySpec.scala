package com.jejking.rprng.lotteries.de.lotto

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, SystemMaterializer}
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import com.jejking.rprng.rng.EightByteString
import org.apache.commons.math3.random.MersenneTwister
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.{Clock, Month, ZoneId, ZonedDateTime}
import scala.concurrent.duration._

class DrawResultSourceFactorySpec extends AnyFlatSpec with Matchers with ScalaFutures {

  implicit val system = ActorSystem("test")
  implicit val materializer = SystemMaterializer.get(system)

  object EightByteStringGenerator {

    val randomGenerator = new MersenneTwister()

    def generateEightByteString(): EightByteString = {
      val eightBytes = Array[Byte](0,0,0,0,0,0,0,0)
      randomGenerator.nextBytes(eightBytes)
      EightByteString(ByteString(eightBytes))
    }
  }

  "the draw result source factory" should "create a source of draw results" in {
    val eightByteStringSource: Source[EightByteString, NotUsed] =
      Source.fromIterator(() => Iterator.continually(EightByteStringGenerator.generateEightByteString()))

    val drawResultSourceFactory = new DrawResultSourceFactory(eightByteStringSource)
    val drawResultSource = drawResultSourceFactory.drawResultSource()

    val drawResultFuture = drawResultSource.take(1).runWith(Sink.head)
    whenReady(drawResultFuture) {
      drawResult => drawResult shouldBe a [DrawResult] // just to check we get through ;)
    }

  }

  "the initial delay" should "be 30.5 seconds when 29.5 seconds off a whole minute" in {
    val time = ZonedDateTime.of(2017, Month.SEPTEMBER.getValue(), 19, 22, 28, 29, 500000000, ZoneId.of("Europe/Berlin"))
    val clock = Clock.fixed(time.toInstant, ZoneId.of("Europe/Berlin"))

    DrawResultSourceFactory.computeInitialDelay(clock) shouldBe (30500 millis)

  }

  it should "be 60 seconds when on called on a whole minute" in {
    val time = ZonedDateTime.of(2017, Month.SEPTEMBER.getValue(), 19, 22, 0, 0, 0, ZoneId.of("Europe/Berlin"))
    val clock = Clock.fixed(time.toInstant, ZoneId.of("Europe/Berlin"))

    DrawResultSourceFactory.computeInitialDelay(clock) shouldBe (60 seconds)
  }

}
