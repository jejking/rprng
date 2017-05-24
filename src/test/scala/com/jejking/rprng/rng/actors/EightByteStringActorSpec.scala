package com.jejking.rprng.rng.actors

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.testkit.{DefaultTimeout, ImplicitSender, TestActorRef, TestKit}
import akka.util.ByteString
import com.jejking.rprng.rng._
import org.scalamock.scalatest.MockFactory
import org.scalamock.MockFactoryBase
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps


/**
  * Test of functionality around [[EightByteStringRngActor]].
  */
class EightByteStringActorSpec extends TestKit(ActorSystem("test")) with DefaultTimeout with ImplicitSender
  with FlatSpecLike with Matchers with BeforeAndAfterAll with MockFactory with Eventually with ScalaFutures  {

  import Protocol._

  implicit override val patienceConfig = PatienceConfig(timeout = 1 second, interval = 100 milliseconds)

  private val eightNotVeryRandomBytes = EightByteString(ByteString(Array[Byte](1, 2, 3, 4, 5, 6, 7, 8)))
  private val timeRange = TimeRangeToReseed(1 milliseconds, 2 milliseconds)

  "the random byte source actor" should "respond with bytes from the wrapped byte source in response to a request" in {

    val mockByteSource = mock[RandomEightByteStringGenerator]

    inAnyOrderWithLogging { // or inSequenceWithLogging
      (mockByteSource.seed _).expects(*)
      (mockByteSource.randomEightByteString _).expects().returning(eightNotVeryRandomBytes)
    }



    val fixedSecureSeeder = stub[SecureSeeder]
    (fixedSecureSeeder.generateSeed _).when().returning(Seed(0L))
    val actorRef = TestActorRef(EightByteStringRngActor.props(mockByteSource, fixedSecureSeeder, TimeRangeToReseed()))

    // send request for EightByteString
    actorRef ! EightByteStringRequest
    expectMsg(eightNotVeryRandomBytes)

  }

  it should "initialise itself from a proper seed source" in {
    val mockByteSource = mock[RandomEightByteStringGenerator]
    val mockSecureSeeder = mock[SecureSeeder]
    (mockSecureSeeder.generateSeed _).expects().returning(Seed(0L))
    (mockByteSource.seed _).expects(*)
    (mockByteSource.randomEightByteString _).expects().returning(eightNotVeryRandomBytes)
    val actorRef = TestActorRef(EightByteStringRngActor.props(mockByteSource, mockSecureSeeder, timeRange))
    val future = actorRef ? EightByteStringRequest
  }

  it should "schedule a message to itself to reseed" in {
    val mockByteSource = mock[RandomEightByteStringGenerator]
    val mockSecureSeeder = mock[SecureSeeder]

    (mockSecureSeeder.generateSeed _).expects().returning(Seed(0))
    (mockByteSource.seed _).expects(*)

    (mockByteSource.randomEightByteString _).expects().returning(eightNotVeryRandomBytes)


    val mockScheduleHelper = mock[ScheduleHelper]
    // we expect that the scheduler is called to send a reseed message between min and max duration from now...
    // the compiler warning emitted here is misleading as it doesn't quite get the particular combination
    // of mocking and an apparently pure function
    (mockScheduleHelper.scheduleOnce(_ : FiniteDuration)(_ : () => Unit)(_ : ExecutionContext)) expects (where {
      (finiteDuration: FiniteDuration, *, ec: ExecutionContext) => finiteDuration === timeRange.minLifeTime
    })


    val scheduleHelperFactory: ActorSystem => ScheduleHelper = _ => mockScheduleHelper

    val actorRef = TestActorRef(EightByteStringRngActor.props(mockByteSource, mockSecureSeeder, scheduleHelperFactory, timeRange))
  }

  it should "obtain new seed - in a way that does not block message processing" in {

    val timeRange = TimeRangeToReseed(1 milliseconds, 2 milliseconds)

    val mockByteSource = mock[RandomEightByteStringGenerator]

    (mockByteSource.randomEightByteString _).expects().returning(eightNotVeryRandomBytes)
    (mockByteSource.seed _).expects(Seed(0))

    val mockSecureSeeder = mock[SecureSeeder]
    (mockSecureSeeder.generateSeed _).expects().returning(Seed(0L)).atLeastTwice()
    val actorRef = TestActorRef(EightByteStringRngActor.props(mockByteSource, mockSecureSeeder, timeRangeToReseed = timeRange))

    Thread.sleep(250) // wait for the async stuff to happen before evaluating the expectations

  }

  it should "apply new seed in a thread-safe way" in {
    val mockByteSource = mock[RandomEightByteStringGenerator]

    (mockByteSource.randomEightByteString _).expects().returning(eightNotVeryRandomBytes)

    (mockByteSource.seed _).expects(*).atLeastTwice()

    val mockSecureSeeder = mock[SecureSeeder]
    (mockSecureSeeder.generateSeed _).expects().returning(Seed(0L)).atLeastTwice()
    val actorRef = TestActorRef(EightByteStringRngActor.props(mockByteSource, mockSecureSeeder, timeRange))

    Thread.sleep(250) // wait for the async stuff to happen before evaluating the expectations
  }



  it should "politely ignore other message types" in {
    // create actor ref with byte source that wraps the fixed source
    val mockByteSource = mock[RandomEightByteStringGenerator]

    (mockByteSource.randomEightByteString _).expects().returning(eightNotVeryRandomBytes)
    (mockByteSource.seed _).expects(*)

    val fixedSecureSeeder = stub[SecureSeeder]
    (fixedSecureSeeder.generateSeed _).when().returning(Seed(0L))
    val actorRef = TestActorRef(EightByteStringRngActor.props(mockByteSource, fixedSecureSeeder, timeRange))

    // send request for four "random" bytes
    val future = actorRef ? "Hello"

    future.value.get.get shouldBe UnknownInputType
  }

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }
}
