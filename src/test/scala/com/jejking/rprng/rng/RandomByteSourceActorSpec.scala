package com.jejking.rprng.rng

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, DefaultTimeout, TestKit, TestActorRef}
import akka.pattern.ask
import akka.util.ByteString
import org.apache.commons.math3.random.{MersenneTwister, RandomGenerator}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.concurrent.duration._

/**
 * Test of functionality around [[RandomByteSourceActor]].
 */
class RandomByteSourceActorSpec extends TestKit(ActorSystem("test")) with DefaultTimeout with ImplicitSender
  with FlatSpecLike with Matchers with BeforeAndAfterAll with MockFactory  {

  import RandomByteSourceActor._

  "the random byte source actor" should "send a message with 4 fixed bytes when assembled with fixed source in response to a request" in {
    // create actor ref with byte source that wraps the fixed source
    val fixedByteSource = RandomByteSource(FixedApacheRandomGenerator())
    val actorRef = TestActorRef(new RandomByteSourceActor(fixedByteSource))

    // send request for four "random" bytes
    val future = actorRef ? RandomByteRequest(4)

    // expect a response back that contains 4 zeros. Expect an immutable ByteString back, not a mutable array
    val expected = ByteString(0, 0, 0, 0)

    val actual = future.value.get.get

    actual shouldBe expected
    actual shouldBe a [ByteString]
  }

  it should "initialise itself from a proper seed source" in {
    fail("not done")
  }

  it should "schedule a message to itself to reseed" in {
    fail("not done")
  }

  it should "obtain new seed in a way that does not block message processing" in {
    fail("not done")
  }

  it should "apply new seed in a thread-safe way" in {
    fail("not done")
  }

  it should "politely ignore other message types" in {
    // create actor ref with byte source that wraps the fixed source
    val fixedByteSource = RandomByteSource(FixedApacheRandomGenerator())
    val actorRef = TestActorRef(new RandomByteSourceActor(fixedByteSource))

    // send request for four "random" bytes
    val future = actorRef ? "Hello"

    future.value.get.get shouldBe UnknownInputType
  }

  "the companion object" should "compute an appropriate schedule" in {
    val randomGenerator = FixedApacheRandomGenerator(0d)
    val minLifeTime: FiniteDuration = 1 minute
    val maxLifeTime: FiniteDuration = 2 minutes

    val lifeSpanRange = LifeSpanRange(minLifeTime, maxLifeTime)
    computeScheduledTimeOfDeath(lifeSpanRange, randomGenerator) shouldBe (1 minute)

    val mersenneTwister = new MersenneTwister()

    for (i <- 1 to 100) {
      val computedScheduledTimeOfDeath = computeScheduledTimeOfDeath(lifeSpanRange, mersenneTwister)
      assert(computedScheduledTimeOfDeath >= minLifeTime)
      assert(computedScheduledTimeOfDeath <= maxLifeTime)
    }
  }

  "LifeSpanRange" should "provide a default min and max lifetime" in {
    val lifeSpanRange = LifeSpanRange()
    lifeSpanRange.minLifeTime shouldBe defaultMinLifeTime
    lifeSpanRange.maxLifeTime shouldBe defaultMaxLifeTime
  }

  it should "accept input where min is less than max" in {
    LifeSpanRange(1 minute, 2 minutes)
  }

  it should "reject input where min is greater than max" in {
    intercept[IllegalArgumentException] {
      LifeSpanRange(1 hour, 1 minute)
    }
  }

  it should "reject input where min is equal to max" in {
    intercept[IllegalArgumentException] {
      LifeSpanRange(1 minute, 1 minute)
    }
  }

  /*
  "the companion object" should "compute a kill time given a random generator and a min and max lifetime" in {
    val randomGenerator = FixedApacheRandomGenerator()
    val minLifeTime: FiniteDuration = 1 minute
    val maxLifeTime: FiniteDuration = 2 minutes
    val config = RandomByteSourceRouterActorConfig[FixedApacheRandomGenerator](6, minLifeTime, maxLifeTime)
    computeScheduledTimeOfDeath(config, randomGenerator) shouldBe (1 minute)

    val mersenneTwister = new MersenneTwister()

    for (i <- 1 to 100) {
      val computedScheduledTimeOfDeath = computeScheduledTimeOfDeath(config, mersenneTwister)
      assert(computedScheduledTimeOfDeath >= minLifeTime)
      assert(computedScheduledTimeOfDeath <= maxLifeTime)
    }
  }


  it should "send a kill signal to children at a random point between their min and max lifetimes" in {
    val minLifeTime: FiniteDuration = 0 seconds
    val maxLifeTime: FiniteDuration = 0.5 seconds
    val config = RandomByteSourceRouterActorConfig[Well19937c](1, minLifeTime, maxLifeTime)
    val probe = TestProbe()

    val actorRef = TestActorRef(RandomByteSourceRouterActor.props(byteSourceFactory, config))

    doWithActorRef(actorRef) {
      val childActorRef = actorRef.children.head
      probe.watch(childActorRef)
      probe.expectTerminated(childActorRef, maxLifeTime + (0.1 seconds))
    }

  }

  it should "initialise a replacement child under on being notified that a child has crashed or been stopped gracefully" in {

    val minLifeTime: FiniteDuration = 1 seconds
    val maxLifeTime: FiniteDuration = 3 seconds
    val config = RandomByteSourceRouterActorConfig[FixedApacheRandomGenerator](1, minLifeTime, maxLifeTime)

    val actorRef = TestActorRef(RandomByteSourceRouterActor.props(byteSourceFactory, config))

    doWithActorRef(actorRef) {
      // we expect the thing to be started twice, once on start, a second time after notification of termination of the first
      EventFilter.debug(pattern = ".*started \\(com.jejking.rprng.rng.RandomByteSourceActor.*", occurrences = 2) intercept {}
    }

  }

  it should "handle a request for random bytes" in {
    val config = RandomByteSourceRouterActorConfig[FixedApacheRandomGenerator](1)
    val actorRef = TestActorRef(RandomByteSourceRouterActor.props(byteSourceFactory, config))

    doWithActorRef(actorRef) {
      whenReady((actorRef ? RandomByteRequest(4)).mapTo[ByteString]) {
        bs: ByteString => bs should be (ByteString(0, 0, 0, 0))
      }
    }

  }
   */

  override def afterAll(): Unit = {
    shutdown()
  }
}
