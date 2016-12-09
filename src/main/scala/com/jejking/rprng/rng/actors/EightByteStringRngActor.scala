package com.jejking.rprng.rng.actors

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.jejking.rprng.rng.actors.Protocol.Reseed
import com.jejking.rprng.rng.{RandomEightByteStringGenerator, Rng, SecureSeeder}

/**
  * Actor encapsulating a [[RandomEightByteStringGenerator]] for thread safety and with additional
  * capability to seed and then - at random intervals - reseed the underlying PRNG.
  */
class EightByteStringRngActor(private val randomEightByteStringGenerator: RandomEightByteStringGenerator,
                              private val secureSeeder: SecureSeeder,
                              private val scheduleHelperCreator: (ActorSystem) => ScheduleHelper = a => new AkkaScheduleHelper(a.scheduler),
                              private val timeRangeToReseed: TimeRangeToReseed = TimeRangeToReseed()) extends Actor with ActorLogging {

  private val scheduleHelper = scheduleHelperCreator(context.system)
  private val actorPath = context.self.path
  private implicit val ec = context.system.dispatcher

  override def preStart(): Unit = {
    val seed = secureSeeder.generateSeed()
    this.randomEightByteStringGenerator.seed(seed)
    scheduleReseed()
    log.info(s"completed pre-start of $actorPath")
  }


  override def receive: Receive = ???

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