package com.jejking.rprng.rng

import akka.NotUsed
import akka.actor.{Actor, ActorSelection, ActorSystem, Props}
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{ActorMaterializer, Graph, SourceShape}
import akka.util.ByteString
import com.jejking.rprng.rng.actors.Protocol.EightByteStringRequest
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Inspectors, Matchers}
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.Future

/**
  * Tests for [[ByteStringSource]].
  */
class ByteStringSourceSpec extends FlatSpec with Matchers with Inspectors with ScalaFutures with BeforeAndAfterAll {

  val singleByteString = ByteString(1)
  val eightByteString = ByteString(1, 2, 3, 4, 5, 6, 7, 8)

  implicit val system = ActorSystem("test")
  implicit val materializer = ActorMaterializer()

  override def beforeAll(): Unit = {
    system.actorOf(Props[NotVeryRandomRngActor], "notveryrandom")
  }

  "the source" should "return a single Future[ByteString] of size 8 when asked" in {
    val actorSelection = system.actorSelection("/user/notveryrandom")
    val sourceGraph: Graph[SourceShape[ByteString], NotUsed] = new ByteStringSource(actorSelection, 8)

    val mySource: Source[ByteString, NotUsed] = Source.fromGraph(sourceGraph)

    val futureByteString: Future[ByteString] = mySource.take(1).toMat(Sink.head)(Keep.right).run
    whenReady(futureByteString) {
      ebs => ebs shouldBe eightByteString
    }
  }

  it should "return a single Future[ByteString] of size 1 when asked" in {
    val actorSelection = system.actorSelection("/user/notveryrandom")
    val sourceGraph: Graph[SourceShape[ByteString], NotUsed] = new ByteStringSource(actorSelection, 1)

    val mySource: Source[ByteString, NotUsed] = Source.fromGraph(sourceGraph)

    val futureByteString: Future[ByteString] = mySource.take(1).toMat(Sink.head)(Keep.right).run
    whenReady(futureByteString) {
      ebs => ebs shouldBe singleByteString
    }
  }

  it should "return a series of 3 Future[ByteString] of size 8 when asked" in {
    val actorSelection = system.actorSelection("/user/notveryrandom")
    val sourceGraph: Graph[SourceShape[ByteString], NotUsed] = new ByteStringSource(actorSelection, 8)

    val mySource: Source[ByteString, NotUsed] = Source.fromGraph(sourceGraph)

    val futureSeqByteString: Future[Seq[ByteString]] = mySource.take(3).runWith(Sink.seq)
    whenReady(futureSeqByteString) {
      futureSeqByteString => futureSeqByteString should contain theSameElementsAs Seq(eightByteString, eightByteString, eightByteString)
    }
  }


  override def afterAll(): Unit = {
    system.terminate()
  }


}

class NotVeryRandomRngActor extends Actor {
  override def receive: Receive = {
    case RandomByteRequest(1) => sender() ! ByteString(1)
    case RandomByteRequest(8) => sender() ! ByteString(1, 2, 3, 4, 5, 6, 7, 8)
  }
}

