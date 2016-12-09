package com.jejking.rprng.rng.actors

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.testkit.{DefaultTimeout, ImplicitSender, TestActorRef, TestKit}
import akka.util.ByteString
import com.jejking.rprng.rng._
import org.apache.commons.math3.random.MersenneTwister
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * Test of functionality around [[RngActor]].
 */
class RngActorSpec extends TestKit(ActorSystem("test")) with DefaultTimeout with ImplicitSender
  with FlatSpecLike with Matchers with BeforeAndAfterAll with MockFactory with Eventually with ScalaFutures  {

  import Protocol._

  implicit override val patienceConfig = PatienceConfig(timeout = 1 second, interval = 100 milliseconds)

  private val fourNotVeryRandomBytes = Array[Byte](1, 2, 3, 4)
  private val eightNotVeryRandomBytes = Array[Byte](1, 2, 3, 4, 5, 6, 7, 8)

  "the random byte source actor" should "respond with bytes from the wrapped byte source in response to a request" in {

    val request = RandomByteRequest(4)


    val mockByteSource = mock[Rng]
    (mockByteSource.randomBytes _).expects(where {
      (request: RandomByteRequest) => request.count == 8
    }).returning(eightNotVeryRandomBytes)
    (mockByteSource.reseed _).expects(*)
    (mockByteSource.randomBytes _).expects(where {
      (request: RandomByteRequest) => request.count == 4
    }).returning(fourNotVeryRandomBytes)

    val fixedSecureSeeder = stub[SecureSeeder]
    val actorRef = system.actorOf(RngActor.props(mockByteSource, fixedSecureSeeder))

    // send request for four "random" bytes
    actorRef ! request
    expectMsg(ByteString(fourNotVeryRandomBytes))

  }

  it should "request and return a random integer on receiving RandomAnyIntRequest" in {

    val mockRandomSource = mock[Rng]
    (mockRandomSource.randomBytes _).expects(where {
      (request: RandomByteRequest) => request.count == 8
    }).returning(eightNotVeryRandomBytes)
    (mockRandomSource.nextInt _).expects().returning(1234)
    (mockRandomSource.reseed _).expects(*)

    val fixedSecureSeeder = stub[SecureSeeder]
    val actorRef = TestActorRef(new RngActor(mockRandomSource, fixedSecureSeeder))

    // send request for any random int
    val response = (actorRef ? RandomAnyIntRequest).mapTo[Int]

    whenReady(response) { i =>
      i should be (1234)
    }

  }

  it should "request and return an appropriate random integer on receiving a RandomIntRequeest" in {

    val mockRandomSource = mock[Rng]
    (mockRandomSource.nextInt (_:RandomIntRequest)).expects(RandomIntRequest(10, 20)).returning(19)
    (mockRandomSource.reseed _).expects(*)
    (mockRandomSource.randomBytes _).expects(where {
      (request: RandomByteRequest) => request.count == 8
    }).returning(eightNotVeryRandomBytes)


    val fixedSecureSeeder = stub[SecureSeeder]
    val actorRef = TestActorRef(new RngActor(mockRandomSource, fixedSecureSeeder))

    // send request for any random int
    val response = (actorRef ? RandomIntRequest(10, 20)).mapTo[Int]

    whenReady(response) { i =>
      i should be (19)
    }

  }

  it should "initialise itself from a proper seed source" in {
    val mockByteSource = mock[Rng]
    val mockSecureSeeder = mock[SecureSeeder]
    (mockSecureSeeder.generateSeed _).expects()
    (mockByteSource.reseed _).expects(*)
    (mockByteSource.randomBytes _).expects(where {
      (request: RandomByteRequest) => request.count == 8
    }).returning(eightNotVeryRandomBytes)
    val actorRef = TestActorRef(new RngActor(mockByteSource, mockSecureSeeder))
    val future = actorRef ? RandomByteRequest(4)
  }

  it should "schedule a message to itself to reseed" in {
    val mockByteSource = mock[Rng]
    (mockByteSource.reseed _).expects(*)

    (mockByteSource.randomBytes _).expects(where {
      (request: RandomByteRequest) => request.count == 8
    }).returning(TestUtils.arrayOfEightZeroBytes)
    

    val mockSecureSeeder = mock[SecureSeeder]
    (mockSecureSeeder.generateSeed _).expects().returning(0)

    val mockScheduleHelper = mock[ScheduleHelper]
    // we expect that the scheduler is called to send a reseed message between min and max duration from now...
    // the compiler warning emitted here is misleading as it doesn't quite get the particular combination
    // of mocking and an apparently pure function
    (mockScheduleHelper.scheduleOnce(_ : FiniteDuration)(_ : () => Unit)(_ : ExecutionContext)) expects (where {
      (finiteDuration: FiniteDuration, *, ec: ExecutionContext) => finiteDuration === TimeRangeToReseed.defaultMinLifeTime
    })


    val scheduleHelperFactory: ActorSystem => ScheduleHelper = _ => mockScheduleHelper

    val actorRef = TestActorRef(new RngActor(mockByteSource, mockSecureSeeder, scheduleHelperFactory))
  }

  it should "obtain new seed - in a way that does not block message processing" in {

    val timeRange = TimeRangeToReseed(1 milliseconds, 2 milliseconds)

    val mockByteSource = mock[Rng]
    (mockByteSource.randomBytes _).expects(where {
      (request: RandomByteRequest) => request.count == 8
    }).returning(eightNotVeryRandomBytes)
    (mockByteSource.reseed _).expects(0)

    val mockSecureSeeder = mock[SecureSeeder]
    (mockSecureSeeder.generateSeed _).expects().atLeastTwice()
    val actorRef = TestActorRef(new RngActor(mockByteSource, mockSecureSeeder, timeRangeToReseed = timeRange))

    Thread.sleep(250) // wait for the async stuff to happen before evaluating the expectations

  }

  it should "apply new seed in a thread-safe way" in {
    val timeRange = TimeRangeToReseed(1 milliseconds, 2 milliseconds)

    val mockByteSource = mock[Rng]
    (mockByteSource.randomBytes _).expects(where {
      (request: RandomByteRequest) => request.count == 8
    }).returning(eightNotVeryRandomBytes)

    (mockByteSource.reseed _).expects(*).atLeastTwice()

    val mockSecureSeeder = mock[SecureSeeder]
    (mockSecureSeeder.generateSeed _).expects().atLeastTwice()
    val actorRef = TestActorRef(new RngActor(mockByteSource, mockSecureSeeder, timeRangeToReseed = timeRange))

    Thread.sleep(250) // wait for the async stuff to happen before evaluating the expectations
  }



  it should "politely ignore other message types" in {
    // create actor ref with byte source that wraps the fixed source
    val mockByteSource = mock[Rng]
    (mockByteSource.randomBytes _).expects(where {
      (request: RandomByteRequest) => request.count == 8
    }).returning(eightNotVeryRandomBytes)

    (mockByteSource.reseed _).expects(*)

    val fixedSecureSeeder = stub[SecureSeeder]
    val actorRef = TestActorRef(new RngActor(mockByteSource, fixedSecureSeeder))

    // send request for four "random" bytes
    val future = actorRef ? "Hello"

    future.value.get.get shouldBe UnknownInputType
  }

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }
}
