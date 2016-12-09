package com.jejking.rprng.rng

import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import com.jejking.rprng.rng.TestUtils.{FailureActor, InsecureSeeder, ZeroRng}
import com.jejking.rprng.rng.actors.RngActor
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

/**
  * Tests for [[EightByteStringActorPublisher]].
  */
class EightByteStringActorPublisherSpec extends FlatSpec with Matchers with BeforeAndAfterAll with MockFactory {

  implicit val system = initActorSystem()
  implicit val materializer = ActorMaterializer()

  def initActorSystem(): ActorSystem = {

    val actorSystem = ActorSystem("publisherSpec")
    actorSystem.actorOf(RngActor.props(new ZeroRng, new InsecureSeeder), "secureSeeder")
    actorSystem.actorOf(Props[FailureActor], "failure")
    actorSystem
  }
  /*
  "the actor publisher" should "send on an eight byte strings when there is demand and it receives one" in {
    fail("not implemented")
  }

  it should "not send on an eight byte string if the publisher has no demand" in {
    fail("not implemented")
  }

  it should "not send on an eight byte string if the publisher is not active" in {
    fail("not implemented")
  }

  it should "call onError with a Throwable received in a Failure message if not in state 'error emitted'" in {
    fail("not implemented")
  }

  it should "not call onError with a Throwable received in a Failure message if in state 'error emitted'" in {
    fail("not implemented")
  }

  it should "fire off requests to the wrapped actor path corresponding to the amount of Demand received" in {
    fail("not implemented")
  }

  it should "stop itself when it receives the Cancel message" in {
    fail("not implemented")
  }

  it should "log a warning if receives something else, e.g. a String" in {
    fail("not implemented")
  }

  */
  override def afterAll(): Unit = {
    this.system.terminate()
  }

}
