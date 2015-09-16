package com.jejking.rprng.rng

import akka.actor._
import akka.stream.{ActorMaterializer}
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import akka.util.ByteString
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

/**
 * Tests for [[RandomByteStringActorPublisher]].
 */
class RandomByteStringActorPublisherSpec extends FlatSpec with Matchers with BeforeAndAfterAll with MockFactory {

  implicit val system = initActorSystem()
  implicit val materializer = ActorMaterializer()

  def initActorSystem(): ActorSystem = {

    val randomByteSource = stub[RandomByteSource]
    (randomByteSource.randomBytes _).when(*).returns(Array(0.toByte, 0.toByte, 0.toByte, 0.toByte, 0.toByte, 0.toByte, 0.toByte, 0.toByte))
    val secureSeeder = stub[SecureSeeder]
    (secureSeeder.generateSeed _).when().returns(0L)

    val actorSystem = ActorSystem("publisherSpec")
    actorSystem.actorOf(RandomByteSourceActor.props(randomByteSource, secureSeeder), "secureSeeder")
    actorSystem
  }

  override def afterAll(): Unit = {
    this.system.shutdown()
  }

  "the random byte string actor publisher" should "provide a stream of byte strings from wrapped actor path" in {

    val source = Source.actorPublisher[ByteString](RandomByteStringActorPublisher.props(8, "/user/secureSeeder"))
    source.runWith(TestSink.probe[ByteString]).request(1).expectNext(ByteString(Array(0.toByte, 0.toByte, 0.toByte, 0.toByte, 0.toByte, 0.toByte, 0.toByte, 0.toByte)))

  }

  it should "notify the subscriber if attempt to get a byte string from the wrapped actor path fails" in {
    fail("not done")
  }

  it should "stop itself when it receives a cancel message from the subscriber" in {
    fail("not done")
  }

}
