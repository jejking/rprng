package com.jejking.rprng.rng.actors


import akka.actor._
import akka.util.ByteString
import com.jejking.rprng.rng._

import scala.concurrent.{Future, blocking}
import scala.language.postfixOps

/**
 * Actor wrapping a [[Rng]] to allow thread-safe access to it and to manage its lifecycle,
 * especially with regard to re-seeding.
 *
 * @param rng PRNG wrapped by actor
 * @param secureSeeder source for higher-quality seed for the PRNG
 */
class RngActor(private val rng: Rng, private val secureSeeder: SecureSeeder,
               private val scheduleHelperCreator: (ActorSystem) => ScheduleHelper = a => new AkkaScheduleHelper(a.scheduler),
               private val timeRangeToReseed: TimeRangeToReseed = TimeRangeToReseed()) extends Actor with ActorLogging {

  import Protocol._

  import scala.concurrent.ExecutionContext.Implicits.global

  private val scheduleHelper  =  scheduleHelperCreator(context.system)
  private val actorPath = context.self.path


  override def preStart(): Unit = {
    log.info(s"reseed interval is: $timeRangeToReseed")
    val seed = secureSeeder.generateSeed()
    this.rng.reseed(seed)
    scheduleReseed()
    log.info("completed pre-start of " + actorPath)

  }

  override def receive: Receive = {

    // main use case, fetch random bytes on demand
    case r: RandomByteRequest => {
      sender() ! ByteString(rng.randomBytes(r))
      if (log.isDebugEnabled) {
        log.debug("processed request for " + r.count + "bytes")
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
    this.rng.reseed(newSeed)
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
    val timeToReseed = TimeRangeToReseed.durationToReseed(timeRangeToReseed, rng)
    scheduleHelper.scheduleOnce(timeToReseed) {
      self ! Reseed
      log.info("sent reseed message to actor " + actorPath)
    }
  }
}

/**
 * Defines constants and helper function.
 */
object RngActor {

  /**
   * Assembles Akka Props for the actor to avoid closing over actor state.
   * @param randomByteSource a random byte source that will be wrapped by the actor.
   * @param secureSeeder a secure seeder to use to fetch initial seeding of the random byte source and for subsequent reseeding
   * @return akka props
   */
  def props(randomByteSource: Rng, secureSeeder: SecureSeeder): Props = Props(new RngActor(randomByteSource, secureSeeder))


  /**
   * Assembles Akka Props for the actor.
   * @param randomByteSource a random byte source that will be wrapped by the actor.
   * @param secureSeeder a secure seeder to use to fetch initial seeding of the random byte source and for subsequent reseeding
   * @param timeRangeToReseed specified time range to reseed
   * @return akka props
   */
  def props(randomByteSource: Rng, secureSeeder: SecureSeeder, timeRangeToReseed: TimeRangeToReseed): Props =
    Props(new RngActor(rng = randomByteSource, secureSeeder = secureSeeder, timeRangeToReseed = timeRangeToReseed))


}
