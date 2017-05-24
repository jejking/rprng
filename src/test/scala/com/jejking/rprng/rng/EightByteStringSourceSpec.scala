package com.jejking.rprng.rng

import akka.NotUsed
import akka.actor.{Actor, ActorSelection, ActorSystem, Props}
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{ActorMaterializer, Graph, SourceShape}
import akka.util.ByteString
import com.jejking.rprng.rng.actors.Protocol.EightByteStringRequest
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Inspectors, Matchers}
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.duration._
import scala.concurrent.Future

/**
  * Tests for [[EightByteStringSource]].
  */
class EightByteStringSourceSpec extends FlatSpec with Matchers with Inspectors with ScalaFutures with BeforeAndAfterAll {

  //implicit override val patienceConfig = PatienceConfig(timeout = 1 second, interval = 100 milliseconds)
  implicit val system = ActorSystem("test")
  implicit val materializer = ActorMaterializer()

  override def beforeAll(): Unit = {
    system.actorOf(Props[NotVeryRandomEightByteActor], "notveryrandom")
  }

  "the source" should "return a series of Future[EightByteString] when asked" in {
    val actorSelection = system.actorSelection("/user/notveryrandom")
    val sourceGraph: Graph[SourceShape[EightByteString], NotUsed] = new EightByteStringSource(actorSelection)

    val mySource: Source[EightByteString, NotUsed] = Source.fromGraph(sourceGraph)

    val futureEightByteStrings: Future[EightByteString] = mySource.take(1).toMat(Sink.head)(Keep.right).run
    whenReady(futureEightByteStrings) {
      ebs => ebs shouldBe EightByteString(ByteString(1,2,3,4,5,6,7,8))
    }
  }
  // can we hook up a dummy actor that just returns EightByteString(1,2,3,4,5,7,8)?

  // pass it into the graph stage

  // should get Future[EightByteString] out

  // should mapAsync down to what we want


  override def afterAll(): Unit = {
    system.terminate()
  }


}

class NotVeryRandomEightByteActor extends Actor {
  override def receive: Receive = {
    case EightByteStringRequest => sender() ! EightByteString(ByteString(1, 2, 3, 4, 5, 6, 7, 8))
  }
}

