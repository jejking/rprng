package com.jejking.rprng.rng.actors

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.util.ByteString
import com.jejking.rprng.rng.actors.Protocol.{EightByteStringRequest, Reseed, UnknownInputType}
import com.jejking.rprng.rng._

import scala.concurrent.{Future, blocking}

/**
  * Actor encapsulating a [[RandomEightByteStringGenerator]] for thread safety and with additional
  * capability to seed via a [[SecureSeeder]] and then - at random intervals - reseed the underlying PRNG.
  *
  * @param randomEightByteStringGenerator underlying generator that is wrapped with the thread safety and scheduling
  *                                       capabilities of the actor
  * @param secureSeeder source of secure randomness which is used to seed - and then reseed - the
  *                     underlying generator
  * @param scheduleHelperCreator abstraction over ability to schedule actions (here reseeding) in the
  *                              future
  * @param timeRangeToReseed abstraction over minimum and maximum time between which a random reseeding time
  *                          is selected at which to schedule a reseed of the underlying generator.
  */
class EightByteStringRngActor(private val randomEightByteStringGenerator: RandomEightByteStringGenerator,
                              private val secureSeeder: SecureSeeder,
                              private val scheduleHelperCreator: (ActorSystem) => ScheduleHelper = a => new AkkaScheduleHelper(a.scheduler),
                              private val timeRangeToReseed: TimeRangeToReseed = TimeRangeToReseed()) extends Actor with ActorLogging {

  private val scheduleHelper = scheduleHelperCreator(context.system)
  private val actorPath = context.self.path
  private implicit val ec = context.system.dispatcher

  override def preStart(): Unit = {
    val theSeed = secureSeeder.generateSeed()
    this.randomEightByteStringGenerator.seed(theSeed)
    scheduleReseed()
    log.info(s"completed pre-start of $actorPath")
    println(s"Did preStart() of $actorPath")

  }

  override def receive: Receive = {
    // main use case, fetch random bytes on demand
    case EightByteStringRequest => {
      println("Got EightByteStringRequest")
      //val eightByteString = randomEightByteStringGenerator.randomEightByteString()

      sender() ! EightByteString(ByteString(1, 2, 3 ,4, 5 , 6, 7, 8))
      //println(s"Sent EightByteString $eightByteString")
      if (log.isDebugEnabled) {
        log.debug("processed EightByteStringRequest")
      }
    }

    // trigger reseed
    case Reseed => fetchSeedAndNotify()

    // apply fresh seed, non blocking action
    case newSeed: Seed => applyNewSeedAndScheduleReseed(newSeed)

    // ooops
    case _ => {
      log.warning("received unknown message type")
      sender() ! UnknownInputType
    }
  }

  def applyNewSeedAndScheduleReseed(newSeed: Seed): Unit = {
    this.randomEightByteStringGenerator.seed(newSeed)
    log.info("applied new seed in actor " + actorPath)

    // and off we go for the next round at some point in the future
    scheduleReseed()
  }

  def fetchSeedAndNotify(): Unit = {
    log.info("about to trigger future to collect fresh seed for actor " + actorPath)
    // generate seed is likely to block as the seeder gathers entropy
    // therefore we do this in a future and send a message onto the actor's mailbox
    // for async application once we have a result
    Future {
      blocking {
        val seed = secureSeeder.generateSeed()
        log.info("obtained fresh seed in actor " + actorPath)
        self ! seed
      }

    }
  }

  def scheduleReseed(): Unit = {
    // non-blocking computation
    val timeToReseed = TimeRangeToReseed.durationToReseed(timeRangeToReseed, randomEightByteStringGenerator)
    scheduleHelper.scheduleOnce(timeToReseed) {
      self ! Reseed
      log.info(s"sent reseed message to actor $actorPath")
    }
    log.info(s"scheduled reseed message for actor $actorPath in $timeToReseed")
  }

}

object EightByteStringRngActor {

  /**
    * Assembles Akka Props for the actor.
    *
    * @param randomEightByteStringGenerator a random generator of [[com.jejking.rprng.rng.EightByteString]]
    *                                       that will be wrapped by the actor.
    * @param secureSeeder                   a secure seeder to use to fetch initial seeding of the
    *                                       random byte source and for subsequent reseeding
    * @param timeRangeToReseed              specified time range to reseed
    * @return akka props
    */
  def props(randomEightByteStringGenerator: RandomEightByteStringGenerator, secureSeeder: SecureSeeder, timeRangeToReseed: TimeRangeToReseed): Props =
    Props(new EightByteStringRngActor(
      randomEightByteStringGenerator = randomEightByteStringGenerator,
      secureSeeder = secureSeeder,
      timeRangeToReseed = timeRangeToReseed))


  /**
    * Assembles Akka Props for the actor.
    *
    * @param randomEightByteStringGenerator a random generator of [[com.jejking.rprng.rng.EightByteString]]
    *                                       that will be wrapped by the actor.
    * @param secureSeeder                   a secure seeder to use to fetch initial seeding of the
    *                                       random byte source and for subsequent reseeding
    * @param scheduleHelperCreator          function to create a [[ScheduleHelper]] from an [[ActorSystem]]
    * @param timeRangeToReseed              specified time range to reseed
    * @return akka props
    */
  def props(randomEightByteStringGenerator: RandomEightByteStringGenerator, secureSeeder: SecureSeeder, scheduleHelperCreator: (ActorSystem) => ScheduleHelper, timeRangeToReseed: TimeRangeToReseed): Props =
    Props(new EightByteStringRngActor(
      randomEightByteStringGenerator = randomEightByteStringGenerator,
      secureSeeder = secureSeeder,
      scheduleHelperCreator = scheduleHelperCreator,
      timeRangeToReseed = timeRangeToReseed))

}