package com.jejking.rprng.rng.actors

import akka.actor.{Cancellable, Scheduler}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

/**
 * Standard implementation that simply delegates to the akka scheduler.
  *
  * @param scheduler reference to an akka scheduler to delegate to
 */
class AkkaScheduleHelper(scheduler: Scheduler) extends ScheduleHelper {

  override def scheduleOnce(delay: FiniteDuration)(f: ⇒ Unit)(implicit executor: ExecutionContext): Cancellable =  {
    scheduler.scheduleOnce(delay)(f)
  }
}
