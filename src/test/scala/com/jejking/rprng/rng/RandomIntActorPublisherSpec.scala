package com.jejking.rprng.rng

import akka.actor._
import akka.stream.ActorMaterializer
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.TestProbe
import com.jejking.rprng.rng.TestUtils.{FailureActor, InsecureSeeder, ZeroRandomSource}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

/**
 * Tests for [[RandomIntActorPublisher]].
 */
class RandomIntActorPublisherSpec extends FlatSpec with Matchers with BeforeAndAfterAll with MockFactory {

  implicit val system = initActorSystem()
  implicit val materializer = ActorMaterializer()

  def initActorSystem(): ActorSystem = {

    val actorSystem = ActorSystem("publisherSpec")
    actorSystem.actorOf(RandomSourceActor.props(new ZeroRandomSource, new InsecureSeeder), "secureSeeder")
    actorSystem.actorOf(Props[FailureActor], "failure")
    actorSystem
  }


  "the random int actor publisher" should "provide a stream of ints from wrapped actor path" in {

    val source = Source.actorPublisher[Int](RandomIntActorPublisher.props(RandomIntRequest(0, 10), "/user/secureSeeder"))
    source.runWith(TestSink.probe[Int]).request(1).expectNext(0)

  }

  it should "notify the subscriber if attempt to get an int from the wrapped actor path fails" in {
    val source = Source.actorPublisher[Int](RandomIntActorPublisher.props(RandomIntRequest(0, 10), "/user/failure"))
    source.runWith(TestSink.probe[Int]).request(1).expectError()
  }

  it should "stop itself when it receives a cancel message from the subscriber" in {
    val newActorRef = system.actorOf(RandomIntActorPublisher.props(RandomIntRequest(0, 10), "/user/secureSeeder"))
    val probe = TestProbe()
    probe watch newActorRef
    val publisher = ActorPublisher[Int](newActorRef)
    val source = Source(publisher)
    source.runWith(TestSink.probe[Int]).cancel()

    probe.expectTerminated(newActorRef)
  }


  override def afterAll(): Unit = {
    this.system.terminate()
  }
}

