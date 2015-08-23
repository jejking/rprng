package com.jejking.rprng.rng

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, DefaultTimeout, TestKit, TestActorRef}
import akka.pattern.ask
import akka.util.ByteString
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, FlatSpec, Matchers}

/**
 * Created by jking on 23/08/15.
 */
class RandomByteSourceActorSpec extends TestKit(ActorSystem("test")) with DefaultTimeout with ImplicitSender
  with FlatSpecLike with Matchers with BeforeAndAfterAll with ByteStringEquality {

  import RandomByteSourceActor._

  "the random byte source actor" should "send a message with 4 fixed bytes when assembled with fixed source in response to a request" in {
    // create actor ref with byte source that wraps the fixed source
    val fixedByteSource = CommonsMathRandomByteSource(FixedApacheRandomGenerator())
    val actorRef = TestActorRef(new RandomByteSourceActor(fixedByteSource))

    // send request for four "random" bytes
    val future = actorRef ? RandomByteRequest(4)

    // expect a response back that contains 4 zeros. Expect an immutable ByteString back, not a mutable array
    val expected = ByteString(0, 0, 0, 0)

    val actual = future.value.get.get

    actual shouldBe expected
    actual shouldBe a [ByteString]
  }

  it should "politely ignore other message types" in {
    // create actor ref with byte source that wraps the fixed source
    val fixedByteSource = CommonsMathRandomByteSource(FixedApacheRandomGenerator())
    val actorRef = TestActorRef(new RandomByteSourceActor(fixedByteSource))

    // send request for four "random" bytes
    val future = actorRef ? "Hello"

    future.value.get.get shouldBe UnknownInputType
  }

  override def afterAll(): Unit = {
    shutdown()
  }
}
