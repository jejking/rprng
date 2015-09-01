package com.jejking.rprng.rng

import java.util.UUID
import java.util.concurrent.TimeUnit

import akka.actor.{ActorRefFactory, ActorContext, ActorRef, ActorSystem}
import akka.event.Logging.LogEvent
import akka.testkit._
import akka.util.ByteString
import com.jejking.rprng.rng.RandomByteSourceActor.GeneratorClass
import com.typesafe.config.ConfigFactory
import org.apache.commons.math3.random.{Well1024a, Well512a, Well19937a, Well19937c, Well44497a, Well44497b, MersenneTwister}
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.{ScalaFutures, Futures}
import org.scalatest.{BeforeAndAfterAll, Matchers, FlatSpecLike}

import akka.pattern.{GracefulStopSupport, ask}

import scala.concurrent.{Future, Await}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._


/**
 * Created by jking on 23/08/15.
 */
class RandomByteSourceRouterActorSpec extends TestKit(ActorSystem("test", ConfigFactory.parseString(
  """
    |akka {
    |  loglevel = "DEBUG"
    |  actor {
    |    debug {
    |      receive = on
    |      autoreceive = on
    |      lifecycle = on
    |    }
    |  }
    |  loggers = ["akka.testkit.TestEventListener"]
    |}
  """.stripMargin))) with DefaultTimeout with ImplicitSender with GracefulStopSupport
  with FlatSpecLike with Matchers with BeforeAndAfterAll with ScalaFutures with MockFactory {

  import RandomByteSourceRouterActor._
  import scala.concurrent.ExecutionContext.Implicits.global
  import org.scalamock.scalatest.proxy.MockFactory


  val byteSourceFactory = new RandomByteSourceFactory(new FixedSeedGeneratingSecureRandom())

  "the router actor" should "be constructable with a byte source factory and create a single child actor" in {
    val actorRef = TestActorRef(RandomByteSourceRouterActor.props(byteSourceFactory, RandomByteSourceRouterActorConfig[MersenneTwister](1)))
    actorRef.children.size should be (1)
    this.system.stop(actorRef)
  }

  it should "be configurable with type of Apache Commons Math Random Generator" in {
    val actorRef = TestActorRef(RandomByteSourceRouterActor.props(byteSourceFactory, RandomByteSourceRouterActorConfig[Well1024a](4)))
    for (child <- actorRef.children) {

      val future = (child ? GeneratorClass).mapTo[Class[_]]
      whenReady(future) { c:Class[_] =>
        c shouldBe new Well1024a().getClass
      }

    }
  }

  it should "be configurable with number of children to generate" in {
    val actorRef = TestActorRef(RandomByteSourceRouterActor.props(byteSourceFactory, RandomByteSourceRouterActorConfig[Well512a](6)))
    doWithActorRef(actorRef) {
      actorRef.children.size should be (6)
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

  it should "not dispatch incoming requests for random bytes to any child actors which have been told to stop" in {
    fail("not done yet")

    val minLifeTime: FiniteDuration = 0 seconds
    val maxLifeTime: FiniteDuration = 0.5 seconds
    val config = RandomByteSourceRouterActorConfig[Well44497b](1, minLifeTime, maxLifeTime)
    val actorRef = TestActorRef(RandomByteSourceRouterActor.props(byteSourceFactory, config))
    doWithActorRef(actorRef) {
      val probe = TestProbe()
      probe.watch(this.system.deadLetters)
      probe.expectNoMsg()
    }

  }

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

  override def afterAll(): Unit = {
    shutdown()
  }

  def doWithActorRef(actorRef: ActorRef)(body: => Unit): Unit = {
    try {
      body
    } catch {
      case e: Exception => fail(e)
    } finally {
      Await.result(gracefulStop(actorRef, 1 second), 1 second)
    }
  }

}
