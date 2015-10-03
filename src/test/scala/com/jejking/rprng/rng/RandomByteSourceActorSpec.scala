package com.jejking.rprng.rng

import akka.actor.{Scheduler, ActorSystem}
import akka.testkit.{ImplicitSender, DefaultTimeout, TestKit, TestActorRef}
import akka.pattern.ask
import akka.util.ByteString
import org.apache.commons.math3.random.{MersenneTwister, RandomGenerator}
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
 * Test of functionality around [[RandomByteSourceActor]].
 */
class RandomByteSourceActorSpec extends TestKit(ActorSystem("test")) with DefaultTimeout with ImplicitSender
  with FlatSpecLike with Matchers with BeforeAndAfterAll with MockFactory with Eventually  {

  import RandomByteSourceActor._
  import RandomByteSourceActor.Protocol._
  import scala.concurrent.ExecutionContext.Implicits.global

  "the random byte source actor" should "send a message with 4 fixed bytes when assembled with fixed source in response to a request" in {
    // create actor ref with byte source that wraps the fixed source
    val fixedByteSource = RandomGeneratorByteSource(FixedApacheRandomGenerator())
    val fixedSecureSeeder = new SecureRandomSeeder(new FixedSeedGeneratingSecureRandom())
    val actorRef = TestActorRef(new RandomByteSourceActor(fixedByteSource, fixedSecureSeeder))

    // send request for four "random" bytes
    val future = actorRef ? RandomByteRequest(4)

    // expect a response back that contains 4 zeros. Expect an immutable ByteString back, not a mutable array
    val expected = ByteString(0, 0, 0, 0)

    val actual = future.value.get.get

    actual shouldBe expected
    actual shouldBe a [ByteString]
  }

  it should "initialise itself from a proper seed source" in {
    val mockByteSource = mock[RandomByteSource]
    val mockSecureSeeder = mock[SecureSeeder]
    (mockSecureSeeder.generateSeed _).expects()
    (mockByteSource.reseed _).expects(*)
    val actorRef = TestActorRef(new RandomByteSourceActor(mockByteSource, mockSecureSeeder))
    val future = actorRef ? RandomByteRequest(4)
  }

  it should "schedule a message to itself to reseed" in {
    val mockByteSource = stub[RandomByteSource]
    

    val mockSecureSeeder = mock[SecureSeeder]
    (mockSecureSeeder.generateSeed _).expects()

    val mockScheduleHelper = mock[ScheduleHelper]
    // we expect that the scheduler is called to send a reseed message between min and max duration from now...
    (mockScheduleHelper.scheduleOnce(_ : FiniteDuration)(_ : () => Unit)(_ : ExecutionContext)) expects (where {
      (finiteDuration: FiniteDuration, f: () => Unit, executor: ExecutionContext) => finiteDuration === defaultMinLifeTime
    })


    val scheduleHelperFactory: ActorSystem => ScheduleHelper = _ => mockScheduleHelper

    val actorRef = TestActorRef(new RandomByteSourceActor(mockByteSource, mockSecureSeeder, scheduleHelperFactory))
  }

  it should "obtain new seed - in a way that does not block message processing" in {

    val timeRange = TimeRangeToReseed(1 milliseconds, 2 milliseconds)

    val mockByteSource = stub[RandomByteSource]
    (mockByteSource.nextInt _).when(*).returns(0)

    val mockSecureSeeder = mock[SecureSeeder]
    (mockSecureSeeder.generateSeed _).expects().atLeastTwice()
    val actorRef = TestActorRef(new RandomByteSourceActor(mockByteSource, mockSecureSeeder, timeRangeToReseed = timeRange))

    Thread.sleep(100) // wait for the async stuff to happen before evaluating the expectations

  }

  it should "apply new seed in a thread-safe way" in {
    val timeRange = TimeRangeToReseed(1 milliseconds, 2 milliseconds)

    val mockByteSource = mock[RandomByteSource]
    (mockByteSource.nextInt (_: Int)).expects(*)
    (mockByteSource.reseed _).expects(*).atLeastTwice()

    val mockSecureSeeder = mock[SecureSeeder]
    (mockSecureSeeder.generateSeed _).expects().atLeastTwice()
    val actorRef = TestActorRef(new RandomByteSourceActor(mockByteSource, mockSecureSeeder, timeRangeToReseed = timeRange))

    Thread.sleep(100) // wait for the async stuff to happen before evaluating the expectations
  }

  it should "politely ignore other message types" in {
    // create actor ref with byte source that wraps the fixed source
    val fixedByteSource = RandomGeneratorByteSource(FixedApacheRandomGenerator())
    val fixedSecureSeeder = new SecureRandomSeeder(new FixedSeedGeneratingSecureRandom())
    val actorRef = TestActorRef(new RandomByteSourceActor(fixedByteSource, fixedSecureSeeder))

    // send request for four "random" bytes
    val future = actorRef ? "Hello"

    future.value.get.get shouldBe UnknownInputType
  }



  "LifeSpanRange" should "provide a default min and max lifetime" in {
    val lifeSpanRange = TimeRangeToReseed()
    lifeSpanRange.minLifeTime shouldBe defaultMinLifeTime
    lifeSpanRange.maxLifeTime shouldBe defaultMaxLifeTime
  }

  it should "accept input where min is less than max" in {
    TimeRangeToReseed(1 minute, 2 minutes)
  }

  it should "reject input where min is greater than max" in {
    intercept[IllegalArgumentException] {
      TimeRangeToReseed(1 hour, 1 minute)
    }
  }

  it should "reject input where min is equal to max" in {
    intercept[IllegalArgumentException] {
      TimeRangeToReseed(1 minute, 1 minute)
    }
  }

  "the companion object" should "compute an appropriate schedule" in {
    val byteSource = new RandomGeneratorByteSource(FixedApacheRandomGenerator(0d))
    val minLifeTime: FiniteDuration = 1 minute
    val maxLifeTime: FiniteDuration = 2 minutes

    val lifeSpanRange = TimeRangeToReseed(minLifeTime, maxLifeTime)
    computeScheduledTimeToReseed(lifeSpanRange, byteSource) shouldBe (1 minute)

    val mersenneTwister = new MersenneTwister()

    for (i <- 1 to 100) {
      val computedScheduledTimeOfDeath = computeScheduledTimeToReseed(lifeSpanRange, new RandomGeneratorByteSource(mersenneTwister))
      assert(computedScheduledTimeOfDeath >= minLifeTime)
      assert(computedScheduledTimeOfDeath <= maxLifeTime)
    }
  }


  it should "compute a reseed time given a random byte source and a time range to use" in {
    val randomGenerator = FixedApacheRandomGenerator()
    val byteSource = RandomGeneratorByteSource(randomGenerator)
    val minLifeTime: FiniteDuration = 1 minute
    val maxLifeTime: FiniteDuration = 2 minutes
    val config = TimeRangeToReseed(minLifeTime, maxLifeTime)
    computeScheduledTimeToReseed(config, byteSource) shouldBe (1 minute)

    val mersenneTwister = new MersenneTwister()
    val mtByteSource = RandomGeneratorByteSource(mersenneTwister)

    for (i <- 1 to 100) {
      val computedScheduledTimeOfDeath = computeScheduledTimeToReseed(config, mtByteSource)
      assert(computedScheduledTimeOfDeath >= minLifeTime)
      assert(computedScheduledTimeOfDeath <= maxLifeTime)
    }
  }

  override def afterAll(): Unit = {
    shutdown()
  }
}
