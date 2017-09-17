package com.jejking.rprng.lotteries.de.lotto

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import com.jejking.rprng.rng.EightByteString
import org.apache.commons.math3.random.MersenneTwister
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}

class DrawResultSourceFactorySpec extends FlatSpec with Matchers with ScalaFutures {

  implicit val system = ActorSystem("test")
  implicit val materializer = ActorMaterializer()

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

}
