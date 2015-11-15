package com.jejking.rprng.rng

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.testkit.{DefaultTimeout, ImplicitSender, TestActorRef, TestKit}
import akka.util.ByteString
import org.apache.commons.math3.random.MersenneTwister
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.{ScalaFutures, Eventually}
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
 * Test of functionality around [[RandomSourceActor]].
 */
class RandomSourceActorSpec extends TestKit(ActorSystem("test")) with DefaultTimeout with ImplicitSender
  with FlatSpecLike with Matchers with BeforeAndAfterAll with MockFactory with Eventually with ScalaFutures  {

  import RandomSourceActor.Protocol._
  import RandomSourceActor._

  "the random byte source actor" should "send respond with bytes from the wrapped byte source in response to a request" in {

    val request = RandomByteRequest(4)
    val notVeryRandomBytes = Array[Byte](1, 2, 3, 4)

    val mockByteSource = mock[RandomSource]
    (mockByteSource.randomBytes _).expects(where {
      (request: RandomByteRequest) => request.count == 4
    }).returning(notVeryRandomBytes)
    (mockByteSource.reseed _).expects(*)

    (mockByteSource.nextInt (_: Int)).expects(*)


    val fixedSecureSeeder = stub[SecureSeeder]
    val actorRef = TestActorRef(new RandomSourceActor(mockByteSource, fixedSecureSeeder))

    // send request for four "random" bytes
    val future = actorRef ? request

    val actual = future.value.get.get

    actual shouldBe notVeryRandomBytes
    actual shouldBe a [ByteString]
  }

  it should "request and return a random integer on receiving RandomAnyIntRequest" in {

    val mockRandomSource = mock[RandomSource]
    (mockRandomSource.nextInt _).expects().returning(1234)
    (mockRandomSource.reseed _).expects(*)
    (mockRandomSource.nextInt (_: Int)).expects(*)

    val fixedSecureSeeder = stub[SecureSeeder]
    val actorRef = TestActorRef(new RandomSourceActor(mockRandomSource, fixedSecureSeeder))

    // send request for any random int
    val response = (actorRef ? RandomAnyIntRequest).mapTo[Int]

    whenReady(response) { i =>
      i should be (1234)
    }

  }

  it should "initialise itself from a proper seed source" in {
    val mockByteSource = mock[RandomSource]
    val mockSecureSeeder = mock[SecureSeeder]
    (mockSecureSeeder.generateSeed _).expects()
    (mockByteSource.reseed _).expects(*)
    val actorRef = TestActorRef(new RandomSourceActor(mockByteSource, mockSecureSeeder))
    val future = actorRef ? RandomByteRequest(4)
  }

  it should "schedule a message to itself to reseed" in {
    val mockByteSource = mock[RandomSource]
    (mockByteSource.nextInt (_:Int)).expects(*).returning(0)
    (mockByteSource.reseed _).expects(0L)
    

    val mockSecureSeeder = mock[SecureSeeder]
    (mockSecureSeeder.generateSeed _).expects()

    val mockScheduleHelper = mock[ScheduleHelper]
    // we expect that the scheduler is called to send a reseed message between min and max duration from now...
    (mockScheduleHelper.scheduleOnce(_ : FiniteDuration)(_ : () => Unit)(_ : ExecutionContext)) expects (where {
      (finiteDuration: FiniteDuration, *, executor: ExecutionContext) => finiteDuration === defaultMinLifeTime
    })


    val scheduleHelperFactory: ActorSystem => ScheduleHelper = _ => mockScheduleHelper

    val actorRef = TestActorRef(new RandomSourceActor(mockByteSource, mockSecureSeeder, scheduleHelperFactory))
  }

  it should "obtain new seed - in a way that does not block message processing" in {

    val timeRange = TimeRangeToReseed(1 milliseconds, 2 milliseconds)

    val mockByteSource = stub[RandomSource]
    (mockByteSource.nextInt _).when().returns(0)

    val mockSecureSeeder = mock[SecureSeeder]
    (mockSecureSeeder.generateSeed _).expects().atLeastTwice()
    val actorRef = TestActorRef(new RandomSourceActor(mockByteSource, mockSecureSeeder, timeRangeToReseed = timeRange))

    Thread.sleep(100) // wait for the async stuff to happen before evaluating the expectations

  }

  it should "apply new seed in a thread-safe way" in {
    val timeRange = TimeRangeToReseed(1 milliseconds, 2 milliseconds)

    val mockByteSource = mock[RandomSource]
    (mockByteSource.nextInt (_: Int)).expects(*)
    (mockByteSource.reseed _).expects(*).atLeastTwice()

    val mockSecureSeeder = mock[SecureSeeder]
    (mockSecureSeeder.generateSeed _).expects().atLeastTwice()
    val actorRef = TestActorRef(new RandomSourceActor(mockByteSource, mockSecureSeeder, timeRangeToReseed = timeRange))

    Thread.sleep(100) // wait for the async stuff to happen before evaluating the expectations
  }



  it should "politely ignore other message types" in {
    // create actor ref with byte source that wraps the fixed source
    val fixedByteSource = stub[RandomSource]
    val fixedSecureSeeder = stub[SecureSeeder]
    val actorRef = TestActorRef(new RandomSourceActor(fixedByteSource, fixedSecureSeeder))

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
    val byteSource = mock[RandomSource]
    (byteSource.nextInt (_: Int)).expects(60000).returning(0)
    val minLifeTime: FiniteDuration = 1 minute
    val maxLifeTime: FiniteDuration = 2 minutes

    val lifeSpanRange = TimeRangeToReseed(minLifeTime, maxLifeTime)
    computeScheduledTimeToReseed(lifeSpanRange, byteSource) shouldBe (1 minute)

    val mersenneTwister = new MersenneTwister()

    for (i <- 1 to 100) {
      val computedScheduledTimeOfDeath = computeScheduledTimeToReseed(lifeSpanRange, new RandomGeneratorSource(mersenneTwister))
      assert(computedScheduledTimeOfDeath >= minLifeTime)
      assert(computedScheduledTimeOfDeath <= maxLifeTime)
    }
  }

  override def afterAll(): Unit = {
    shutdown()
  }
}
