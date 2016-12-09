package com.jejking.rprng.rng

import akka.actor._
import akka.stream.ActorMaterializer
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.TestProbe
import akka.util.ByteString
import com.jejking.rprng.rng.TestUtils.{FailureActor, InsecureSeeder, ZeroRng}
import com.jejking.rprng.rng.actors.RngActor
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

/**
 * Tests for [[RandomIntActorPublisher]].
 */
class RandomByteStringActorPublisherSpec extends FlatSpec with Matchers with BeforeAndAfterAll with MockFactory {

  implicit val system = initActorSystem()
  implicit val materializer = ActorMaterializer()

  def initActorSystem(): ActorSystem = {

    val actorSystem = ActorSystem("publisherSpec")
    actorSystem.actorOf(RngActor.props(new ZeroRng, new InsecureSeeder), "secureSeeder")
    actorSystem.actorOf(Props[FailureActor], "failure")
    actorSystem
  }


  "the random byte string actor publisher" should "provide a stream of byte strings from wrapped actor path" in {

    val source = Source.actorPublisher[ByteString](RandomByteStringActorPublisher.props(8, "/user/secureSeeder"))
    source.runWith(TestSink.probe[ByteString]).request(1).expectNext(ByteString(Array(0.toByte, 0.toByte, 0.toByte, 0.toByte, 0.toByte, 0.toByte, 0.toByte, 0.toByte)))

  }

  it should "notify the subscriber if attempt to get a byte string from the wrapped actor path fails" in {
    val source = Source.actorPublisher[ByteString](RandomByteStringActorPublisher.props(8, "/user/failure"))
    source.runWith(TestSink.probe[ByteString]).request(1).expectError()
  }

  it should "stop itself when it receives a cancel message from the subscriber" in {
    val newActorRef = system.actorOf(RandomByteStringActorPublisher.props(8, "/user/secureSeeder"))
    val probe = TestProbe()
    probe watch newActorRef
    val publisher = ActorPublisher[ByteString](newActorRef)
    val source = Source.fromPublisher(publisher)
    source.runWith(TestSink.probe[ByteString]).cancel()

    probe.expectTerminated(newActorRef)
  }


  override def afterAll(): Unit = {
    this.system.terminate()
  }
}

